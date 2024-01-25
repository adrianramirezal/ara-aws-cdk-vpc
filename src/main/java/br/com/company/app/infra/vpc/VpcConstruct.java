package br.com.company.app.infra.vpc;

import software.amazon.awscdk.CfnTag;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

import java.util.*;

public class VpcConstruct extends Construct {

    private final CfnVPC vpc;
    private final List<Subnet> setSubnetPublicWebTiers;
    private final List<Subnet> setSubnetPrivateAppTiers;
    private final List<Subnet> setSubnetPrivateDbTiers;

    public VpcConstruct(final Construct scope, final String id) {
        super(scope, id);

        this.vpc = new CfnVPC(this, "CAPP-VPC",
                CfnVPCProps.builder()
                        .cidrBlock("10.0.0.0/16")
                        .enableDnsSupport(true)
                        .enableDnsHostnames(true)
                        .tags(List.of(CfnTag.builder().key("Name").value("CAPP-VPC").build()))
                        .build()
        );

        // Internet Gateway
        CfnInternetGateway igw = new CfnInternetGateway(vpc, "CAPP-IGW",
                CfnInternetGatewayProps.builder()
                        .tags(Collections.singletonList(CfnTag.builder().key("Name").value("CAPP-IGW").build()))
                        .build()
        );
        new CfnVPCGatewayAttachment(vpc, "CAPP-IGW_ATT",
                CfnVPCGatewayAttachmentProps.builder()
                        .vpcId(vpc.getAttrVpcId())
                        .internetGatewayId(igw.getRef())
                        .build()
        );

        this.setSubnetPublicWebTiers = new ArrayList<>();
        this.setSubnetPrivateAppTiers = new ArrayList<>();
        this.setSubnetPrivateDbTiers = new ArrayList<>();

        // SubNets Public for Web
        Subnet snPublic = Subnet.Builder.create(vpc, "CAPP-SN-PubWeb")
                .vpcId(vpc.getAttrVpcId())
                .cidrBlock("10.0.1.0/24")
                .mapPublicIpOnLaunch(true)
                .availabilityZone(Stack.of(this).getAvailabilityZones().get(0))
                .build();
        setSubnetPublicWebTiers.add(snPublic);

        // SubNets Private for App
        PrivateSubnet snPrvApp = PrivateSubnet.Builder.create(vpc, "CAPP-SN-PrvAPP")
                .vpcId(vpc.getAttrVpcId())
                .cidrBlock("10.0.2.0/24")
                .mapPublicIpOnLaunch(false)
                .availabilityZone(Stack.of(this).getAvailabilityZones().get(0))
                .build();
        setSubnetPrivateAppTiers.add(snPrvApp);

        // SubNets Private for Database
        PrivateSubnet snPrvDB = PrivateSubnet.Builder.create(vpc, "CAPP-SN-PrvDB")
                .vpcId(vpc.getAttrVpcId())
                .cidrBlock("10.0.3.0/24")
                .mapPublicIpOnLaunch(false)
                .availabilityZone(Stack.of(this).getAvailabilityZones().get(0))
                .build();
        setSubnetPrivateDbTiers.add(snPrvDB);

        // Nat Gateway + Elastic IP
        CfnEIP eip = new CfnEIP(snPublic, "NAT_GW-EIP",
                CfnEIPProps.builder()
                        .domain("vpc")
                        .build()
        );
        CfnNatGateway natGW = new CfnNatGateway(eip, "CAPP-NAT_GW",
                CfnNatGatewayProps.builder()
                        .allocationId(eip.getAttrAllocationId())
                        .subnetId(snPublic.getSubnetId())
                        .tags(Collections.singletonList(CfnTag.builder().key("Name").value("CAPP-NAT_GW").build()))
                        .build()
        );

        // Route from public subnet to IGW
        new CfnRoute(snPublic, "snIGWRoute", CfnRouteProps.builder()
                .destinationCidrBlock("0.0.0.0/0")
                .gatewayId(igw.getRef())
                .routeTableId(snPublic.getRouteTable().getRouteTableId())
                .build()
        );

        // Route from subnets to Nat Gateway
        new CfnRoute(snPrvApp, "snPrvAppNGWRoute", CfnRouteProps.builder()
                .destinationCidrBlock("0.0.0.0/0")
                .natGatewayId(natGW.getRef())
                .routeTableId(snPrvApp.getRouteTable().getRouteTableId())
                .build()
        );

        // Route from subnets to Nat Gateway
        new CfnRoute(snPrvDB, "snPrvDBNGWRoute", CfnRouteProps.builder()
                .destinationCidrBlock("0.0.0.0/0")
                .natGatewayId(natGW.getRef())
                .routeTableId(snPrvDB.getRouteTable().getRouteTableId())
                .build()
        );

        CfnNetworkAcl networkAcl = CfnNetworkAcl.Builder.create(vpc, "CAPP-NACL-PubWeb")
                .vpcId(vpc.getAttrVpcId())
                .tags(Collections.singletonList(CfnTag.builder().key("Name").value("CAPP-NACL-PubWeb").build()))
                .build();
        CfnSubnetNetworkAclAssociation.Builder.create(snPublic, "CAPP-NACLA-PubWeb")
                .networkAclId(networkAcl.getAttrId())
                .subnetId(snPublic.getSubnetId())
                .build();

        // NACL Inbound Rules
        CfnNetworkAclEntry.Builder.create(networkAcl, "Allow-IN-All-TCP-Port80")
                .networkAclId(networkAcl.getAttrId())
                .egress(false)
                .ruleNumber(100)
                .portRange(CfnNetworkAclEntry.PortRangeProperty.builder().from(80).to(80).build())
                .protocol(6)
                .cidrBlock("0.0.0.0/0")
                .ruleAction("allow")
                .build();

        // NACL Outbound  Rules
        CfnNetworkAclEntry.Builder.create(networkAcl, "Allow-OUT-All-TCP-Port80")
                .networkAclId(networkAcl.getAttrId())
                .egress(true)
                .ruleNumber(100)
                .portRange(CfnNetworkAclEntry.PortRangeProperty.builder().from(80).to(80).build())
                .protocol(6)
                .cidrBlock("0.0.0.0/0")
                .ruleAction("allow")
                .build();
    }

    public CfnVPC getVpc() {
        return vpc;
    }

    public List<Subnet> getSetSubnetPublicWebTiers() {
        return setSubnetPublicWebTiers;
    }

    public List<Subnet> getSetSubnetPrivateAppTiers() {
        return setSubnetPrivateAppTiers;
    }

    public List<Subnet> getSetSubnetPrivateDbTiers() {
        return setSubnetPrivateDbTiers;
    }
}
