package br.com.company.app.infra;

import software.amazon.awscdk.App;

public final class InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        new AppInfraStack(app, "AppInfraStack");

        app.synth();
    }
}
