package br.com.company.app.infra;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;

import java.util.HashMap;
import java.util.Map;

public class AppInfraStackTest {

    @Test
    public void testVpcStack() {
        App app = new App();
        AppInfraStack stack = new AppInfraStack(app, "AppInfraStackTest");

        Template template = Template.fromStack(stack);

        template.resourceCountIs("AWS::EC2::VPC", 1);
        template.resourceCountIs("AWS::EC2::InternetGateway", 1);
        template.resourceCountIs("AWS::EC2::VPCGatewayAttachment", 1);
        template.resourceCountIs("AWS::EC2::Subnet", 3);
        template.resourceCountIs("AWS::EC2::EIP", 1);
        template.resourceCountIs("AWS::EC2::NatGateway", 1);

        template.hasResourceProperties("AWS::EC2::VPC", new HashMap<String, Boolean>() {{
            put("EnableDnsHostnames", true);
            put("EnableDnsSupport", true);
        }});
    }

    @Test
    public void testRouteResources() {
        App app = new App();
        AppInfraStack stack = new AppInfraStack(app, "AppInfraStackTest");

        Template template = Template.fromStack(stack);

        template.resourceCountIs("AWS::EC2::RouteTable", 3);
        template.resourceCountIs("AWS::EC2::SubnetRouteTableAssociation", 3);
        template.resourceCountIs("AWS::EC2::Route", 3);

        final Capture natRefCapture = new Capture();
        template.hasResourceProperties("AWS::EC2::Route", Map.of(
                "DestinationCidrBlock", "0.0.0.0/0",
                "NatGatewayId", Map.of(
                        "Ref", natRefCapture
                )
        ));

        final Capture igwRefCapture = new Capture();
        template.hasResourceProperties("AWS::EC2::Route", Map.of(
                "DestinationCidrBlock", "0.0.0.0/0",
                "GatewayId", Map.of(
                        "Ref", igwRefCapture
                )
        ));

        // Assert that the start state starts with "Start".
        assert (natRefCapture.asString()).matches("^AppVPCCAPPVPCCAPPSNPubWebNATGWEIPCAPPNATGW.+");
        assert (igwRefCapture.asString()).matches("^AppVPCCAPPVPCCAPPIGW.+");
    }

    @Test
    public void testNACLsResources() {
        App app = new App();
        AppInfraStack stack = new AppInfraStack(app, "AppInfraStackTest");

        Template template = Template.fromStack(stack);
        template.resourceCountIs("AWS::EC2::NetworkAcl", 1);
        template.resourceCountIs("AWS::EC2::SubnetNetworkAclAssociation", 1);
        template.resourceCountIs("AWS::EC2::NetworkAclEntry", 2);

        template.hasResourceProperties("AWS::EC2::NetworkAclEntry", new HashMap<String, Object>() {{
            put("Protocol", 6);
            put("RuleAction", "allow");
            put("RuleNumber", 100);
            put("CidrBlock", "0.0.0.0/0");
            put("Egress", false);
            put("PortRange", Map.of(
                    "From", 80,
                    "To", 80
            ));
        }});

        template.hasResourceProperties("AWS::EC2::NetworkAclEntry", new HashMap<String, Object>() {{
            put("Protocol", 6);
            put("RuleAction", "allow");
            put("RuleNumber", 100);
            put("CidrBlock", "0.0.0.0/0");
            put("Egress", true);
            put("PortRange", Map.of(
                    "From", 80,
                    "To", 80
            ));
        }});
    }
}
