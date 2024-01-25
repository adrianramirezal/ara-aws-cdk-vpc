package br.com.company.app.infra;

import br.com.company.app.infra.vpc.VpcConstruct;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

public class AppInfraStack extends Stack {

    public AppInfraStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public AppInfraStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        VpcConstruct cappVpc = new VpcConstruct(this, "AppVPC");
    }
}
