import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";
import * as pulumi from "@pulumi/pulumi";

const cfg = new pulumi.Config();
const dbPassword = cfg.requireSecret("dbPassword");

// ECR repo for the backend image
const repo = new awsx.ecr.Repository("library-repo", { forceDelete: true });
const image = new awsx.ecr.Image("library-image", {
  repositoryUrl: repo.url,
  context: "../../backend",
  platform: "linux/amd64",
});

// VPC + cluster
const vpc = new awsx.ec2.Vpc("library-vpc", { numberOfAvailabilityZones: 2 });
const cluster = new aws.ecs.Cluster("library-cluster");

// RDS PostgreSQL
const subnetGroup = new aws.rds.SubnetGroup("library-subnets", {
  subnetIds: vpc.privateSubnetIds,
});
const dbSg = new aws.ec2.SecurityGroup("library-db-sg", {
  vpcId: vpc.vpcId,
  ingress: [{ protocol: "tcp", fromPort: 5432, toPort: 5432, cidrBlocks: ["10.0.0.0/16"] }],
  egress: [{ protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] }],
});
const db = new aws.rds.Instance("library-db", {
  engine: "postgres",
  engineVersion: "16.3",
  instanceClass: "db.t4g.micro",
  allocatedStorage: 20,
  dbName: "library",
  username: "library",
  password: dbPassword,
  dbSubnetGroupName: subnetGroup.name,
  vpcSecurityGroupIds: [dbSg.id],
  skipFinalSnapshot: true,
});

// Public ALB-fronted Fargate service
const lb = new awsx.lb.ApplicationLoadBalancer("library-lb", { });

const service = new awsx.ecs.FargateService("library-svc", {
  cluster: cluster.arn,
  assignPublicIp: true,
  desiredCount: 1,
  taskDefinitionArgs: {
    container: {
      name: "library",
      image: image.imageUri,
      cpu: 512,
      memory: 1024,
      essential: true,
      portMappings: [{ containerPort: 8080, targetGroup: lb.defaultTargetGroup }],
      environment: [
        { name: "DB_URL", value: pulumi.interpolate`jdbc:postgresql://${db.endpoint}/library` },
        { name: "DB_USER", value: "library" },
        { name: "DB_PASSWORD", value: dbPassword },
        { name: "JWT_ISSUER", value: cfg.require("jwtIssuer") },
        { name: "JWT_JWKS_URL", value: cfg.require("jwtJwksUrl") },
      ],
    },
  },
});

export const url = pulumi.interpolate`http://${lb.loadBalancer.dnsName}`;
export const dbHost = db.endpoint;
