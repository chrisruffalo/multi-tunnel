package com.github.chrisruffalo.multitunnel.options.tunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IStringConverter;
import com.github.chrisruffalo.multitunnel.model.tunnel.TunnelConfiguration;

public class TunnelConfigurationConverter implements IStringConverter<TunnelConfiguration> {

	private static final String CONFIGUATION_TEXT_FORMAT = "<sourcePort>:<destinationHost>:<destinationPort>\" or \"<sourceInterface>:<sourcePort>:<destinationHost>:<destinationPort>";
	
	@Override
	public TunnelConfiguration convert(String arg0) {
		TunnelConfiguration instance = new TunnelConfiguration();
		
		Logger logger = LoggerFactory.getLogger("configuration");
		
		String[] split = arg0.split(":");
		
		if(split.length < 3 || split.length > 4) {
			// log infraction
			logger.error("Tunnel instance '{}' does not match the format: '{}'", arg0, TunnelConfigurationConverter.CONFIGUATION_TEXT_FORMAT);
			
			// do nothing
			return null;
		}
		
		int offset = 0;
		
		try {
			if(split.length == 4) {
				instance.setSourceInterface(split[0]);
				offset = 1;
			}
			instance.setSourcePort(Integer.parseInt(split[0 + offset]));
			instance.setDestHost(split[1 + offset]);
			instance.setDestPort(Integer.parseInt(split[2 + offset]));
		} catch (Exception ex) {
			// logg error
			logger.error("Could not parse tunnel configuration '{}', tunnel description should be in the format: '{}'", arg0, TunnelConfigurationConverter.CONFIGUATION_TEXT_FORMAT);
			
			// do not add
			return null;
		}
		
		return instance;
	}

}
