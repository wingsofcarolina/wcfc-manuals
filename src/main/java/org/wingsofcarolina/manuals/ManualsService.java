package org.wingsofcarolina.manuals;

import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.TimeZone;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.knowm.dropwizard.sundial.SundialBundle;
import org.knowm.dropwizard.sundial.SundialConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.persistence.Persistence;
import org.wingsofcarolina.manuals.email.EmailLogin;
import org.wingsofcarolina.manuals.common.RuntimeExceptionMapper;
import org.wingsofcarolina.manuals.healthcheck.MinimalHealthCheck;
import org.wingsofcarolina.manuals.resources.ManualsResource;
import org.wingsofcarolina.manuals.resources.MembersResource;
import org.wingsofcarolina.manuals.slack.Slack;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ManualsService extends Application<ManualsConfiguration> {
	private static final Logger LOG = LoggerFactory.getLogger(ManualsService.class);
	
	public static void main(String[] args) throws Exception {
		LOG.info("Starting : WCFC Manuals Server");
        if (args.length < 2) {
            new ManualsService().run(new String[]{"server", "configuration.ftl"});
        } else {
            new ManualsService().run(args);
        }
	}

	@Override
	public void initialize(Bootstrap<ManualsConfiguration> bootstrap) {
		// bootstrap.addBundle(new AssetsBundle("/doc", "/doc", "index.html","html"));
		bootstrap.addBundle(new TemplateConfigBundle(new TemplateConfigBundleConfiguration()));
    	bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
    	bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new SundialBundle<ManualsConfiguration>() {
            @Override
            public SundialConfiguration getSundialConfiguration(ManualsConfiguration configuration) {
              return configuration.getSundialConfiguration();
            }
          });

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
		new Persistence().initialize(config.getMongodb());
		
		// Make sure the email class knows the right server to reference
		EmailLogin.initialize(config.getManualsServer());

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
        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "/");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }
}
