package org.wingsofcarolina.manuals;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.TimeZone;
import org.eclipse.jetty.ee10.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.common.RuntimeExceptionMapper;
import org.wingsofcarolina.manuals.email.EmailLogin;
import org.wingsofcarolina.manuals.healthcheck.MinimalHealthCheck;
import org.wingsofcarolina.manuals.persistence.Persistence;
import org.wingsofcarolina.manuals.resources.ManualsResource;
import org.wingsofcarolina.manuals.resources.MembersResource;
import org.wingsofcarolina.manuals.slack.Slack;

public class ManualsService extends Application<ManualsConfiguration> {

  private static final Logger LOG = LoggerFactory.getLogger(ManualsService.class);

  public static void main(String[] args) throws Exception {
    LOG.info("Starting : WCFC Manuals Server");
    if (args.length < 2) {
      new ManualsService().run(new String[] { "server", "configuration.yml" });
    } else {
      new ManualsService().run(args);
    }
  }

  @Override
  public void initialize(Bootstrap<ManualsConfiguration> bootstrap) {
    // Enable environment variable substitution
    bootstrap.setConfigurationSourceProvider(
      new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
      )
    );

    // bootstrap.addBundle(new AssetsBundle("/doc", "/doc", "index.html","html"));
    bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));

    // Add additional assets bundle for SPA fallback on specific routes
    bootstrap.addBundle(new AssetsBundle("/assets/", "/equipment", "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/", "/contact", "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/", "/about", "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/", "/login", "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/", "/manage", "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/", "/view", "index.html"));
    bootstrap.addBundle(new MultiPartBundle());
  }

  @Override
  public String getName() {
    return "wcfc-manuals";
  }

  @Override
  public void run(ManualsConfiguration config, Environment env) throws Exception {
    // Configure server to require /api before all server calls
    env.jersey().setUrlPattern("/api/*");

    if (config.getAuth()) {
      LOG.info("Authorization enabled");
    } else {
      LOG.info("Authorization disabled");
    }

    // Set up Slack communications
    new Slack(config);

    // Let those who care know we started
    Slack.instance().sendString(Slack.Channel.NOTIFY, "Manuals server started.");

    // Get the startup date/time in GMT
    SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));

    // Configure to allow CORS
    configureCors(env);

    // Set up the Persistence singleton
    new Persistence().initialize(config.getMongodb(), config.getMongodbDatabase());

    // Make sure the email class knows the right server to reference
    EmailLogin.initialize(config.getManualsServer(), config.getGmailImpersonateUser());

    // Set exception mappers
    if (config.getMode().contentEquals("PROD")) {
      env.jersey().register(new RuntimeExceptionMapper());
    }

    // Now set up the API
    env.jersey().register(new ManualsResource(config));
    env.jersey().register(new MembersResource(config));
    env.healthChecks().register("check", new MinimalHealthCheck());
  }

  private void configureCors(Environment environment) {
    final FilterRegistration.Dynamic cors = environment
      .servlets()
      .addFilter("CORS", CrossOriginFilter.class);

    // Configure CORS parameters
    cors.setInitParameter("allowedOrigins", "*");
    cors.setInitParameter(
      "allowedHeaders",
      "X-Requested-With,Content-Type,Accept,Origin,Authorization"
    );
    cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");
    cors.setInitParameter("allowCredentials", "true");

    // Add URL mapping
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
  }
}
