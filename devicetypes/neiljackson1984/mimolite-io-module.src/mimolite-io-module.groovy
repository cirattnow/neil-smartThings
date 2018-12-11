/**
 * copyright 2018 by Neil Jackson
 *
 * adapted from
 *   1) "MimoLite Garage Door Controller" device handler by Todd Wackford (https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/mimolite-garage-door-controller.src/mimolite-garage-door-controller.groovy)
 *   2) "FortrezZ MIMOlite" device handler by Fortrezz (https://github.com/fortrezz/smartthings/blob/master/mimolite/devicehandler.groovy)
 *
 * See the file "Tech+Appendix+MIMOlite+8May2017+removed+MI+address.pdf" published by Fortrezz, the manufacturer of the device.
 * The device supports the following zwave command classes:
 *    MANUFACTURER_SPECIFIC
 *    VERSION
 *    ALARM (V1, for power fail)
 *    SENSOR_BINARY
 *    SENSOR_MULTILEVEL ('General Purpose Value' type for SIG1 analog ADC Count)
 *    METER_PULSE (count incremented based on each new triggering event at SIG1)
 *    CONFIGURATION
 *    ASSOCIATION
 *    SWITCH_BINARY
 *  
 *     
 *    Node Information Frame (NIF): ‘Always Listening’ flag set, ‘Optional Functionality’ flag set
 *    Manufacturer ID 0x0084
 *    Product Type ID 0x0453 (US) 0x0451 (EU)
 *    Generic Device Class GENERIC_TYPE_SWITCH_BINARY
 *    Specific Device Class SPECIFIC_TYPE_NOT_USED
*      
*      
*      
*     Command Class Description
*     COMMAND_CLASS_ALARM – sends unsolicited reports (association group 3) and responds to Gets with an Alarm
*     Type of 0x08 (for power drop warning) and an Alarm Level of 0x00 (no power drop) or 0xFF (power drop)
*     COMMAND_CLASS_SENSOR_BINARY
*     (Indicates input triggering state: high for digital inputs; see Configuration CC, parameter 8)
*     COMMAND_CLASS_SENSOR_MULTILEVEL
*     ('General Purpose Value' type for 12-bit ADC Counts; analog input, SIG1, only)
*     COMMAND_CLASS_METER_PULSE (for input SIG1; count incremented based on each new triggering event)
*     COMMAND_CLASS_ASSOCIATION - Five Association Groups; maximum of two nodes in each group.
*     Group 1: When the input is triggered or untriggered, the MIMOlite will automatically send a Basic Set command to turn on or off the
*     device(s) associated with this group.
*     Group 2: The MIMOlite will periodically (see Parameter 9 of Configuration Command Class below) send a MultiLevel Sensor report
*     indicating the input’s analog voltage level.
*     Group 3: If a power dropout occurs, the MIMOlite will send an Alarm Command Class report (if there is enough available residual
*     power)
*     Group 4: When the input is triggered or untriggered, the MIMOlite will automatically send a Binary Sensor report to this group’s
*     associated device(s).
*     Group 5: Pulse meter counts will be sent to this group’s associated device(s). This will be sent periodically at the same intervals as
*     Association Group 2, MLS Report except that if the pulse meter count is unchanged the report will not be sent.
*     Notes: a) MLS Association Groups 2 and 5 transmissions do not attempt an explorer frame, if nodes do not ACK; b) If triggers occur too
*     quickly, Association Group 1 or 4 reports can block transmissions if sent to a non-responding node; c) Upon a power dropout, the
*     MIMOlite likely only has enough residual power to send to the first node in Association Group 3.
*     COMMAND_CLASS_CONFIGURATION (all parameters are one byte unsigned values or can be considered as a
*     commanded, signed value with an offset of 128)
*     • Parameter 2 is for input SIG1 only.
*     • Parameter 3 (input to relay mapping) and Parameter 11 only apply to the Relay1 output. Parameters 4-
*     10 are for input SIG1 only.
*     • Parameters 4-7 are for analog input characteristics only. The 8-bit thresholds below are used for
*     determining triggering and represent the upper 8 most-significant bits (with lower 4 bits of threshold set
*     to 0) for comparison to the 12-bit Analog-to-Digital converted value.
*      
*      
*      
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "MimoLite io module", namespace: "neiljackson1984", author: "Neil Jackson") {
        fingerprint deviceId: "0x1000", inClusters: "0x72,0x86,0x71,0x30,0x31,0x35,0x70,0x85,0x25,0x03"
        
        //TAGGING CAPABILITIES: ('tagging' implies that these capabilities have no attributes, and have no commands)
        
        capability "Actuator"  //The "Actuator" capability is simply a marker to inform SmartThings that this device has commands     
        //attributes: (none)
        //commands:  (none)
        
        capability "Sensor"   //The "Sensor" capability is simply a marker to inform SmartThings that this device has attributes     
        //attributes: (none)
        //commands:  (none)
        
        
        
		capability "Configuration"
        //attributes: (none)
        //commands: configure()
        
		capability "Polling" 
        //deprecated
        //attributes: (none)
        //commands: poll()
        
		capability "Switch"
        //attributes: enum switch ("on", "off")
        //commands: on(), off()
        
		capability "Refresh"
        //attributes: (none)
        //commands: refresh()
        

		capability "Contact Sensor"
        //attributes: enum contact ("open", "closed")
        //commands: (none)
        
        capability "Voltage Measurement"
        //attributes: voltage (an object that has properties "value" and "unit"
        //commands: (none)
        
        attribute("powered", "enum", ["powerOff", "powerOn"]);
        
        command("runTheTestCode");
        command("resetThePulseCounter");

        attribute("configuration", "string"); //we will use this attribute to record the internal configuration data reported by the device (mainly for debugging and user interest)

        attribute("pulseCount", "number");
        
        //each of the configuration parameters is a single byte, and will be stored as a decimal number string.
        //attribute("configurationParameter01", "string"); //not used
        attribute("configurationParameter02", "string");  //this parameter's value is irrelevant - sending a set command for this parameter (regardless of value) causes the pulse counter to be reset.
        attribute("configurationParameter03", "string");
        attribute("configurationParameter04", "string");
        attribute("configurationParameter05", "string");
        attribute("configurationParameter06", "string");
        attribute("configurationParameter07", "string");
        attribute("configurationParameter08", "string");
        attribute("configurationParameter09", "string");
        //attribute("configurationParameter10", "string"); //not used
        attribute("configurationParameter11", "string");
        
        //LOGGING and DEBUGGING
        attribute("debugMessage", "string");
        attribute("zwaveCommandFromHubToDevice", "string"); //we will update this attribute to record a log of every zwave command that we (i.e. the device handler) send from the hub to the device.  We will update this attribute whenever we return somewthing from a command function (like on(), off(), refresh(), etc.)
        attribute("zwaveCommandFromDeviceToHub", "string"); //we will update this attribute to record a log of every zwave command that we (i.e. the device handler) receive from the device (in practice, this means that we will update this attribute every time the platform calls our parse() function.
        attribute("zwaveCommand", "string"); //we will update this attribute to record a log of every zwave command that we (i.e. the device handler) receive from the device or send to the device.
        
	}
    
    preferences {
       input "RelaySwitchDelay", "decimal", title: "Delay between relay switch on and off in seconds. Only Numbers 0 to 3.0 allowed. 0 value will remove delay and allow relay to function as a standard switch", description: "Numbers 0 to 3.1 allowed.", defaultValue: 0, required: false, displayDuringSetup: true
    }
    

	// UI tile definitions 
	tiles(scale : 2) {

        standardTile("contact", "device.contact", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", defaultState: true, label: "state of the door is unknown"
            state "open", label: 'DOOR IS OPEN', icon: "st.doors.garage.garage-open",  backgroundColor: "#e86d13"
			state "closed", label: 'DOOR IS CLOSED', icon: "st.doors.garage.garage-closed" //backgroundColor: "#00A0DC"
		}
        
        // valueTile("contact", "device.contact", width: 2, height: 2, inactiveLabel: false) {
			// state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			// state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		// }
        

        
        // valueTile("powered", "device.powered", inactiveLabel: false) {
			// state "powerOn", label: "Power On", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			// state "powerOff", label: "Power Off", icon: "st.switches.switch.off", backgroundColor: "#ffa81e"
		// }
        standardTile("powered", "device.powered", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "powerOn", label: "logic power is stable"//,  backgroundColor: "#79b821"
			state "powerOff", label: "logic power is browned-out",  backgroundColor: "#ffa81e"
		}
		
        valueTile("voltage", "device.voltage", width: 2, height: 1) {
            state "input voltage", label:'${name}: ${currentValue} volts', unit:"volts", defaultState: true
        }
        valueTile("pulseCount", "device.pulseCount", width: 2, height: 1) {
            state "pulseCount", label:'${name}: ${currentValue}', unit:"", defaultState: true
        }
        
        standardTile("configure", "device.configure", inactiveLabel: false, /*decoration: "flat"*/) {
			state "configure", label:'configure', action:"configuration.configure", icon:"st.secondary.configure"
		}
        
        standardTile("refresh", "device.switch", inactiveLabel: false/*, decoration: "flat"*/) {
			state "default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        // standardTile("divider", "device.switch", inactiveLabel: false, width: 12, height: 1, decoration: "flat"*/) {
			// state "default", label:' ', defaultState: true, icon:null
		// }
        standardTile("switch", "device.switch", width: 6, height: 2, inactiveLabel: false /*, decoration: "flat"*/) {
			//state "doorClosed", label: "Closed", action: "on", icon: "st.doors.garage.garage-closed", backgroundColor: "#00A0DC"
            //state "doorOpen", label: "Open", action: "on", icon: "st.doors.garage.garage-open", backgroundColor: "#e86d13"
            //state "doorOpening", label: "Opening", action: "on", icon: "st.doors.garage.garage-opening", backgroundColor: "#e86d13"
            //state "doorClosing", label: "Closing", action: "on", icon: "st.doors.garage.garage-closing", backgroundColor: "#00A0DC"
            state "default", defaultState: true, label: "state of switch is unknown"
            state "on", label: "sending a pulse (click here to stop)", action: "switch.off"  /*icon: "st.switches.switch.on", *//*backgroundColor: "#00A0DC",*//* nextState: "attemptingToStopThePulse" */
			state "off", label: "Send a pulse", action: "switch.on" /*nextState: "attemptingToSendAPulse" *///, icon: "st.switches.switch.off" , backgroundColor: "#ffffff"
            state "attemptingToSendAPulse", label: "attempting to send a pulse..."
            state "attemptingToStopThePulse", label: "attempting to stop the pulse..."
        }
        
		// main (["contact", "switch"])
		main ("contact")
		// details(["contact", "switch", "powered", "refresh", "configure"])
		details(["contact", "powered", "voltage", "pulseCount", "switch", "refresh", "configure"])
	}
}

//{ PLATFORM-REQUIRED LIFECYCLE FUNCTIONS (parse(), updated())

    def updated() {
        log.debug "Settings Updated..."
        //device.configure();
        //return logZwaveCommandFromHubToDevice(getCommandsForConfigure()); //I don't think the platform interprets the return value of the updated() function as commands to be sent out.
        sendHubCommand(
            logZwaveCommandFromHubToDevice(getCommandsForConfigure()).collect{new physicalgraph.device.HubAction(it)}
        );
    }


    //==============parsing incoming commands and helper functions
    def parse(String description) {
        //logZwaveCommandFromHubToDevice(zwave.parse(description).format());
       log.debug("parse(${description}) was called");
        def debugMessage = "";
        def debugMessageDelimeter = "\n";
        debugMessage += debugMessageDelimeter*2;
        debugMessage += "parse(${description}) was called" + debugMessageDelimeter;

        def result = [];
        def cmd = zwave.parse(description,  getCommandClassVersionMap());
        logZwaveCommandFromDeviceToHub(cmd);

        
        // if (cmd.CMD == "7105") {				//Mimo sent a power loss report
            // log.debug "Device lost power"
            // sendEvent(name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power")
        // } else {
            // sendEvent(name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power")
        // }
        //The original code from Todd Wackford ( see https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/dc5a17f016a1424694f820fc84cd57895aaa84ec/devicetypes/smartthings/mimolite-garage-door-controller.src/mimolite-garage-door-controller.groovy#L95 )
        // would do sendEvent(name: "powered", value: "powerOn") in response to every incoming zwave command except those for which cmd.CMD == "7105" (i.e. commands of type physicalgraph.zwave.commands.alarmv1.AlarmReport)
        // I think the idea with this scheme was that any non-alarm command indicates that the device is powered and working.  The problem is that a "7105" command could be a report that there is no alarm condition (which the device would send in response to an AlarmGet command).
        // also, in general, (although possibly not with the mimolite io module), the alarm type might be something other than POWER_MANAGEMENT. 
        // In general, it is not ideal to set the powered attribute to powerOn in response to any non-alarm command, because it might conceivably happen that the device would send some regular reports even during a brownout condition, but
        // the thing that makes us want to set powered to powerOn in response to any on-alarm command is that the device does not automatically send a "power restored" alarm report upon first turning on after having been powered off.
        //therefore, in order to provide fast response to regaining power, without having to wait for a scheduled poll, we will regard any signal other than an "alarm is high" report that power is OK.  /in practice, brownouts are unlikely - the more 
        // frequent case is a complete loss of power followed by a complete regaining of power.
        
        
        if (cmd) {
            if(
                !(
                    cmd instanceof physicalgraph.zwave.commands.alarmv1.AlarmReport &&
                    cmd.alarmType == physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT &&
                    cmd.alarmLevel
                 )
            ) {
                //if this command is anything other than an alarm report reporting that power has been lost...
                result << createEvent([name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power"]);
            }
            
            result << createEvent(zwaveEvent(cmd));
        }
        
        
        // try {
            // cmd.class
        // } catch(java.lang.SecurityException e)
        // {
            // debugMessage += e.getMessage() + debugMessageDelimeter;
        // } catch (e)
        // {
            
        // }
        
        // debugMessage +=  "cmd: " + cmd + debugMessageDelimeter;
        //debugMessage +=  "cmd.inspect(): " + cmd.inspect() + debugMessageDelimeter;
        //debugMessage +=  "cmd.getProperties(): " + cmd.getProperties() + debugMessageDelimeter;
        //debugMessage +=  "description.inspect(): " + description.inspect() + debugMessageDelimeter;
        // debugMessage +=  "cmd.format(): " + cmd.inspect() + debugMessageDelimeter;
        // debugMessage +=  "groovy.json.JsonOutput.toJson(cmd): " + groovy.json.JsonOutput.toJson(cmd) + debugMessageDelimeter;
        // debugMessage +=  "groovy.json.JsonOutput.toJson(zwave.parse(description)): " + groovy.json.JsonOutput.toJson(zwave.parse(description)) + debugMessageDelimeter;
        // debugMessage +=  "groovy.json.JsonOutput.toJson(zwave.switchBinaryV1.switchBinaryReport(value:0)): " + groovy.json.JsonOutput.toJson(zwave.switchBinaryV1.switchBinaryReport(value:0)) + debugMessageDelimeter;
        // //the class of cmd is physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport
        // //log.debug "command value is: $cmd.CMD"
        // // log.debug "Parse returned ${result?.descriptionText}"
        // debugMessage +=  "Parse returned ${result}" + debugMessageDelimeter;
        // debugMessage += "zwave.basicV1.basicGet(): " + zwave.basicV1.basicGet().inspect()  + debugMessageDelimeter;
        // debugMessage += "zwave.basicV1.basicGet().format(): " + zwave.basicV1.basicGet().format().inspect()  + debugMessageDelimeter;
        // debugMessage += "zwave.basicV1.basicSet(value: 0xFF): " + zwave.basicV1.basicSet(value: 0xFF).inspect()  + debugMessageDelimeter;
        // debugMessage += "zwave.basicV1.basicSet(value: 0xFF).format(): " + zwave.basicV1.basicSet(value: 0xFF).format().inspect()  + debugMessageDelimeter;
        // debugMessage += "zwave.switchBinaryV1.switchBinaryGet().format(): " + zwave.switchBinaryV1.switchBinaryGet().format().inspect()  + debugMessageDelimeter;
        // debugMessage += "zwave.switchBinaryV1.switchBinaryReport(value:0): " + zwave.switchBinaryV1.switchBinaryReport(value:0).inspect()  + debugMessageDelimeter;
        // debugMessage += "zwave.switchBinaryV1.switchBinaryReport(value:0).format(): " + zwave.switchBinaryV1.switchBinaryReport(value:0).format().inspect()  + debugMessageDelimeter;
        // //the class of zwave.switchBinaryV1.switchBinaryReport(value:0) is physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport

        debugMessage += debugMessageDelimeter;
        //log.debug debugMessage
        
        
        return result;
    }

//}

//{  PARSING-HELPER FUNCTIONS

    def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) { //command class code: BASIC (0x20)
        //as far as I can tell, the device never emits a BasicReport command (at least, it does not respond to a BasicGet command, for which, I think, the proper response would be a BasicReport command.)
        return [name: "switch", value: cmd.value ? "on" : "off"];
    }

    def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) { //command class code: BASIC (0x20)
        //the device is deisgned to send a BasicSet command to devices in association group 1 in response to the sensor value changing
        //the idea is that the device could be controlling other zwave devices in response to the sensor value.
        //This is a bit confusing because basicSet is the command that we send to the device to set the relay state.
        return sensorValueEvent(cmd.value);
        
        //the device does not even seem to respond to a basicGet command, which is a bit odd -- it seems like it would make sense for the device to respond in some meaningful way to a BasicGet command.
    }

    def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) { //command class code: SWITCH_BINARY (0x25)
        log.debug "we received a SwitchBinary report"
        return [name: "switch", value: cmd.value ? "on" : "off"];
    }

    def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) { // 'SENSOR_BINARY': 0x30,
        log.debug "received a binary sensor report"
        sensorValueEvent(cmd.sensorValue)
    }
  
  def zwaveEvent(physicalgraph.zwave.commands.meterpulsev1.MeterPulseReport  cmd) { //  'METER_PULSE': 0x35,
        log.debug "received a meter pulse report";
        return [name:"pulseCount", value: cmd.pulseCount];
    }

    
    
    def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd) { // 0x71: 'NOTIFICATION',
        def debugMessage = ""
        debugMessage += "We received a notification zwave command; " 
        def returnValue = [];
        if(cmd.alarmType == physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT)
        {
            if(cmd.alarmLevel) {
                debugMessage += "we lost power."
                returnValue << [name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power"];
            } else {
                debugMessage += "we regained power."
                returnValue <<  [name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power"];
            }
        } 
        log.debug(debugMessage);
        return returnValue;
    }

    def zwaveEvent (physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) { // 0x31: 'SENSOR_MULTILEVEL',
        // sensorMultilevelReport is used to report the value of the analog voltage for SIG1
        log.debug "received a SensorMultilevelReport"
        def ADCvalue = cmd.scaledSensorValue
        //sendEvent(name: "voltageCounts", value: ADCvalue)
       
        return(CalculateVoltage(cmd.scaledSensorValue));
    }

    def zwaveEvent (physicalgraph.zwave.commands.configurationv1.ConfigurationReport  cmd) { //'CONFIGURATION': 0x70,
        def configuration;
        try{
            configuration = device.currentState("configuration").getJsonValue();
        } catch(e) 
        {
            log.debug "initializing the 'configuration' attribute.";
            sendEvent(name: 'configuration', value: "{}", displayed: false); //we initialize to an empty json object.
            configuration = device.currentState("configuration").getJsonValue();
            log.debug "after initializing, configuration is " + configuration;
        }
        
        
        //update the configuration attribute as needed
        configuration.(cmd.parameterNumber) = cmd.configurationValue;
        
        
        sendEvent(name: 'configuration', value: groovy.json.JsonOutput.toJson(configuration));

    }

    def zwaveEvent(physicalgraph.zwave.Command cmd) { 
        // Handles all Z-Wave commands we aren't interested in
        log.debug("Un-parsed Z-Wave message ${cmd}")
        [:]
    }

    def CalculateVoltage(ADCvalue) {
         def map = [:]
         
         def volt = (((1.5338*(10**-16))*(ADCvalue**5)) - ((1.2630*(10**-12))*(ADCvalue**4)) + ((3.8111*(10**-9))*(ADCvalue**3)) - ((4.7739*(10**-6))*(ADCvalue**2)) + ((2.8558*(10**-3))*(ADCvalue)) - (2.2721*(10**-2)))

        //def volt = (((3.19*(10**-16))*(ADCvalue**5)) - ((2.18*(10**-12))*(ADCvalue**4)) + ((5.47*(10**-9))*(ADCvalue**3)) - ((5.68*(10**-6))*(ADCvalue**2)) + (0.0028*ADCvalue) - (0.0293))
        //log.debug "$cmd.scale $cmd.precision $cmd.size $cmd.sensorType $cmd.sensorValue $cmd.scaledSensorValue"
        def voltResult = volt.round(1)// + "v"
        
        map.name = "voltage"
        map.value = voltResult
        map.unit = "v"
        return map
    }

    def sensorValueEvent(Short value) {
        // sendEvent(name: "contact", value: value ? "open" : "closed")
        return [name: "contact", value: value ? "open" : "closed"]
    }
//}

//{  COMMAND METHODS (this section shall contain all functions that are SmartThings commands as declared by capability() and command() statements in the definition in the metadata)
    def configure() {
        log.debug "Configuring.... " //setting up to monitor power alarm and actuator duration
        
        return logZwaveCommandFromHubToDevice(getCommandsForConfigure());
    }

    def on() {
        return logZwaveCommandFromHubToDevice(
            delayBetween([
               zwave.basicV1.basicSet(value: 0xFF).format(),
               //getCommandsForRefresh(),
               zwave.switchBinaryV1.switchBinaryGet().format()
            ])
        );
    }

    def off() {
        return logZwaveCommandFromHubToDevice(
            delayBetween([
               zwave.basicV1.basicSet(value: 0x00).format(), 
               //getCommandsForRefresh(),
               zwave.switchBinaryV1.switchBinaryGet().format()
            ])
        );
    }

    def poll() {
        log.debug "poll() was run";
        return logZwaveCommandFromHubToDevice(getCommandsForRefresh());
    }

    def refresh() {
        log.debug "refresh() was run"
        // zwave.switchBinaryV1.switchBinaryGet().format()
        return logZwaveCommandFromHubToDevice(getCommandsForRefresh());
    }

    def runTheTestCode(){
        def debugMessage = ""
        debugMessage += "\n\n" + "================================================" + "\n";
        debugMessage += (new Date()).format("yyyy/MM/dd HH:mm:ss.SSS", location.getTimeZone()) + "\n";
        debugMessage += "settings: " + settings + "\n";
        

        // debugMessage += "(new physicalgraph.zwave.Command()): " + (new physicalgraph.zwave.Command()).format() + "\n";
        // try {debugMessage += (new physicalgraph.zwave.Command()).getProperties().toString() + "\n";} catch(e){debugMessage += e.message + "\n";}
        // try {debugMessage += (new physicalgraph.zwave.Command()).metaClass.methods*.name.sort().unique().toString()   + "\n";} catch(e){debugMessage += e.getMessage() + "\n";}


        //this is the "description" argument that the platform passes to the parse() method in response to the zwave command that the device sends to the hub as is it is losing power.
        def description = "zw device: 02, command: 7105, payload: 08 FF";
        debugMessage += zwave.parse(description).toString() + "\n";
        debugMessage += zwave.parse(description).inspect() + "\n";
        debugMessage += zwave.parse(description).getProperties()['class'].toString() + "\n";  //equivalently: debugMessage += zwave.parse(description, [(commandClassCodes['NOTIFICATION']):3]).getProperties()['class'].toString() + "\n";
        // spits out: class physicalgraph.zwave.commands.notificationv3.NotificationReport //see https://graph.api.smartthings.com/ide/doc/zwave-utils.html#notificationV3/notificationReport 
        debugMessage += groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(zwave.parse(description))) + "\n"; //equivalently: debugMessage += groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(zwave.parse(description, [(commandClassCodes['NOTIFICATION']):3]))) + "\n";
        //spits out: 
        //    {
        //        "commandClassId": 113,
        //        "NOTIFICATION_TYPE_ACCESS_CONTROL": 6,
        //        "NOTIFICATION_TYPE_BURGLAR": 7,
        //        "NOTIFICATION_TYPE_RESERVED0": 0,
        //        "NOTIFICATION_TYPE_SMOKE": 1,
        //        "v1AlarmType": 8,
        //        "notificationType": 0,
        //        "sequence": false,
        //        "NOTIFICATION_TYPE_FIRST": 255,
        //        "NOTIFICATION_TYPE_POWER_MANAGEMENT": 8,
        //        "reserved61": 0,
        //        "eventParameter": [
        //            
        //        ],
        //        "NOTIFICATION_TYPE_EMERGENCY": 10,
        //        "payload": [
        //            8,
        //            255,
        //            0,
        //            0,
        //            0,
        //            0,
        //            0
        //        ],
        //        "NOTIFICATION_TYPE_WATER": 5,
        //        "zensorNetSourceNodeId": 0,
        //        "notificationStatus": 0,
        //        "commandId": 5,
        //        "v1AlarmLevel": 255,
        //        "NOTIFICATION_TYPE_CO": 2,
        //        "NOTIFICATION_TYPE_SYSTEM": 9,
        //        "eventParametersLength": 0,
        //        "CMD": "7105",
        //        "event": 0,
        //        "NOTIFICATION_TYPE_CO2": 3,
        //        "NOTIFICATION_TYPE_CLOCK": 11,
        //        "NOTIFICATION_TYPE_HEAT": 4
        //    }
        
        debugMessage += zwave.parse(description, [(commandClassCodes['NOTIFICATION']):1]).getProperties()['class'].toString() + "\n";
        //spits out class physicalgraph.zwave.commands.alarmv1.AlarmReport //see https://graph.api.smartthings.com/ide/doc/zwave-utils.html#alarmV1/alarmReport
        debugMessage += groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(zwave.parse(description, [(commandClassCodes['NOTIFICATION']):1]))) + "\n";
        //spits out 
        //    {
        //        "commandClassId": 113,
        //        "commandId": 5,
        //        "alarmType": 8,
        //        "CMD": "7105",
        //        "alarmLevel": 255,
        //        "payload": [
        //            8,
        //            255
        //        ]
        //    }
        
        
        debugMessage += zwave.parse(description, [(commandClassCodes['NOTIFICATION']):2]).getProperties()['class'].toString() + "\n";
        //spits out class physicalgraph.zwave.commands.alarmv2.AlarmReport  //see https://graph.api.smartthings.com/ide/doc/zwave-utils.html#alarmV2/alarmReport
        debugMessage += groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(zwave.parse(description, [(commandClassCodes['NOTIFICATION']):2]))) + "\n";
        // spits out 
        //    {
        //        "commandClassId": 113,
        //        "ZWAVE_ALARM_TYPE_ACCESS_CONTROL": 6,
        //        "ZWAVE_ALARM_TYPE_EMERGENCY": 10,
        //        "ZWAVE_ALARM_TYPE_RESERVED0": 0,
        //        "ZWAVE_ALARM_TYPE_FIRST": 255,
        //        "ZWAVE_ALARM_TYPE_SMOKE": 1,
        //        "ZWAVE_ALARM_TYPE_HEAT": 4,
        //        "zwaveAlarmEvent": 0,
        //        "numberOfEventParameters": 0,
        //        "ZWAVE_ALARM_TYPE_BURGLAR": 7,
        //        "ZWAVE_ALARM_TYPE_CLOCK": 11,
        //        "zwaveAlarmStatus": 0,
        //        "alarmLevel": 255,
        //        "eventParameter": [
        //            
        //        ],
        //        "ZWAVE_ALARM_TYPE_WATER": 5,
        //        "payload": [
        //            8,
        //            255,
        //            0,
        //            0,
        //            0,
        //            0,
        //            0
        //        ],
        //        "ZWAVE_ALARM_TYPE_CO2": 3,
        //        "zensorNetSourceNodeId": 0,
        //        "commandId": 5,
        //        "ZWAVE_ALARM_TYPE_CO": 2,
        //        "ZWAVE_ALARM_TYPE_SYSTEM": 9,
        //        "zwaveAlarmType": 0,
        //        "ZWAVE_ALARM_TYPE_POWER_MANAGEMENT": 8,
        //        "alarmType": 8,
        //        "CMD": "7105"
        //    }
        

        debugMessage += "physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT: " + physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT + "\n";
        
        // sendHubCommand(
            // logZwaveCommandFromHubToDevice(
                // [
                    // (new physicalgraph.zwave.commands.alarmv1.AlarmGet(alarmType: physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT)).format()
                // ]
            // ).collect{new physicalgraph.device.HubAction(it)}
        // );
        
        debugMessage +=  "(new physicalgraph.zwave.commands.alarmv1.AlarmGet(alarmType: physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT)) instanceof physicalgraph.zwave.Command: " + ((new physicalgraph.zwave.commands.alarmv1.AlarmGet(alarmType: physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT)) instanceof physicalgraph.zwave.Command) + "\n";
        debugMessage +=  "zwave.parse('3105020A005A'): " + zwave.parse('3105020A005A') + "\n";
        debugMessage +=  "zwave.parse('3105020A005A').format(): " + zwave.parse('3105020A005A').format() + "\n";
        
        description='zw device: 02, command: 3003, payload: 00 ';
        def cmd = zwave.parse(description);
        debugMessage +=  "cmd: " + cmd + "\n";
        //spits out: cmd: SensorBinaryReport(sensorValue: 0, sensorType: null)
        debugMessage +=  "cmd.getProperties()['class']: " + cmd.getProperties()['class'] + "\n"; 
        //spits out: cmd.getProperties()['class']: class physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport
        //debugMessage +=  "cmd.format(): " + cmd.format() + "\n"; 
        //throws an exception: groovy.lang.MissingMethodException: No signature of method: static java.lang.Long.toHexString() is applicable for argument types: (null) values: [null]
        debugMessage +=  "cmd.getProperties(): " + cmd.getProperties() + "\n"; 
        //spits out: cmd.getProperties(): [commandClassId:48, SENSOR_TYPE_GLASS_BREAK:13, SENSOR_TYPE_CO:3, SENSOR_TYPE_MOTION:12, sensorType:null, SENSOR_TYPE_DOOR_WINDOW:10, SENSOR_VALUE_DETECTED_AN_EVENT:255, SENSOR_TYPE_AUX:9, SENSOR_TYPE_FREEZE:7, SENSOR_TYPE_CO2:4, SENSOR_TYPE_TAMPER:8, SENSOR_TYPE_SMOKE:2, commandId:3, class:class physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport, SENSOR_TYPE_FIRST:255, SENSOR_TYPE_WATER:6, sensorValue:0, SENSOR_VALUE_IDLE:0, SENSOR_TYPE_TILT:11, CMD:3003, SENSOR_TYPE_HEAT:5, SENSOR_TYPE_GENERAL_PURPOSE:1]
        // notice the null value for sensorType.
       
        cmd = zwave.parse(description, getCommandClassVersionMap());
        debugMessage +=  "cmd: " + cmd + "\n";
        //spits out: SensorBinaryReport(sensorValue: 0)
        debugMessage +=  "cmd.getProperties()['class']: " + cmd.getProperties()['class'] + "\n"; 
        //spits out: cmd.getProperties()['class']: class physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport  
        //(because getCommandClassVersionMap() contains the entry [0x30: 1], because the version of the command class 0x30 (i.e. SensorBinary) that the device supports is version 1.  
        // version 1 of that command class does not have the sensorType property, with the result that, when the command is interpreted as version 2 of the command class, 
        // zwave.parse() sets the sensorType property to null, which causes the format error when format() attempts to convert null into a hex string.  
        //the moral of the story is that it is important to inform zwave.parse() which version of the command it is dealing with by passing it a meaningful
        // second argument that matches the specifications of the device.
        debugMessage +=  "cmd.getProperties(): " + cmd.getProperties() + "\n"; 
        //spits out: cmd.getProperties(): [commandClassId:48, commandId:3, class:class physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport, SENSOR_VALUE_DETECTED_AN_EVENT:255, sensorValue:0, SENSOR_VALUE_IDLE:0, CMD:3003, payload:[0]]
        debugMessage +=  "cmd.format(): " + cmd.format() + "\n"; 
        //spits out: cmd.format(): 300300
        
        debugMessage +=  zwave.parse('3105020A001B', getCommandClassVersionMap()).toString() + "\n";
        sendEvent(name: "debugMessage", value: debugMessage, displayed: false);
        //sendEvent(name: "powered", value: "powerOff");
        //return logZwaveCommandFromHubToDevice(getCommandsForConfigurationDump());
        return null;
    }
//}

//{  OUTBOUND SEQUENCES OF ZWAVE COMMANDS
    
    def getCommandsForConfigure() {
        return delayBetween([
            zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]).format(), // the Smartthings platform will have already set this setting, but we set it again here just to be sure. (and we might even want to remove the hub from association group 1, because the information that the device sends to the hub by virtue of the hub being in association group 1 is entirely redundant with the other association groups (I think))
            zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(), // 	FYI: Group 3: If a power dropout occurs, the MIMOlite will send an Alarm Command Class report 	(if there is enough available residual power)
            zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:[zwaveHubNodeId]).format(), // periodically send a multilevel sensor report of the ADC analog voltage to the input
            zwave.associationV1.associationSet(groupingIdentifier:4, nodeId:[zwaveHubNodeId]).format(), // when the input is digitally triggered or untriggered, snd a binary sensor report
            zwave.associationV1.associationSet(groupingIdentifier:5, nodeId:[zwaveHubNodeId]).format(), // Pulse meter counts will be sent to this group’s associated device(s). This will be sent periodically at the same intervals as Association Group 2, multi-level sensor Report except that if the pulse meter count is unchanged the report will not be sent.
            zwave.configurationV1.configurationSet(configurationValue: [(settings.RelaySwitchDelay*10).toInteger()], parameterNumber: 11, size: 1).format(), // configurationValue for parameterNumber means how many 100ms do you want the relay
                                                                                                                    // to wait before it cycles again / size should just be 1 (for 1 byte.)
            getCommandsForConfigurationDump()                                                                                                        
            //zwave.configurationV1.configurationGet(parameterNumber: 11).format() // gets the new parameter changes. not currently needed. (forces a null return value without a zwaveEvent funciton
        ]);
    }

    def getCommandsForConfigurationDump()
    {
         return delayBetween([
            //parameter 1: not used.
            zwave.configurationV1.configurationGet(parameterNumber: 1).format(),

            //parameter 2: Clear Pulse Meter Counts (actual value is “don’t care”; count gets reset whenever
            // this command is received regardless of value)
            // (I think that this only ever has any effect when we are setting it, not sure what it will return when we get it
            zwave.configurationV1.configurationGet(parameterNumber: 2).format(),

            //parameter 3: Trigger Mapping: 1 = SIG1 triggered/untriggered sets or clears Relay1 (Default=0x00;
            // Refer to description in User Manual under section, Input to Relay Mapping) Note
            // that neither a Basic Report nor a Binary Switch Report is sent when relay is
            // automatically set or cleared by Trigger Mapping.
            zwave.configurationV1.configurationGet(parameterNumber: 3).format(),

            //parameter 4: Lower Threshold, High (Default=0xBB; must be less than Upper Threshold Low and
            // greater than Lower Threshold Low)
            zwave.configurationV1.configurationGet(parameterNumber: 4).format(),

            //parameter 5: Lower Threshold, Low (Default=0xAB)
            zwave.configurationV1.configurationGet(parameterNumber: 5).format(),

            //parameter 6: Upper Threshold, High (Default=0xFF)
            zwave.configurationV1.configurationGet(parameterNumber: 6).format(),

            //parameter 7: Upper Threshold, Low (Default = 0xFE; must be greater than Lower Threshold High and less than Upper Threshold High)
            zwave.configurationV1.configurationGet(parameterNumber: 7).format(),

            //parameter 8: flags 
            // (
            //      Bit1 : Digital-Configuration flag
            //          1=Set Trigger levels for this channel to ‘digital’ thresholds (approx. 1V); Default
            //          0=Set Trigger levels to analog thresholds (see parameters 4 through 7)
            //      Bit0 : Trigger-Between-Thresholds flag (see below)
            //          1 = Set to ‘triggered’ when input falls between thresholds; Default
            //          0 = Set to ‘triggered’ when input falls outside of thresholds
            // )
            zwave.configurationV1.configurationGet(parameterNumber: 8).format(),

            //parameter 9: Periodic send interval of Multilevel Sensor Reports (Association Group 2) and/or
            // Pulse Count Reports (Association Group 5) for SIG1. This parameter has a resolution
            // of 10 seconds; for example, 1 = 10 seconds, 2 = 20 seconds, 3 = 30 seconds (Default),
            // …, 255 = 2550 seconds = 42.5 minutes. A value of 0 disables automatic reporting.
            zwave.configurationV1.configurationGet(parameterNumber: 9).format(),

            //parameter 10: not used
            zwave.configurationV1.configurationGet(parameterNumber: 10).format(),

            //parameter 11: Momentary Relay1 output enable/disable. 0 = disable (Default)
            // 1..255 = enable / value sets the approximate momentary on time in increments
            // of 100msec.
            zwave.configurationV1.configurationGet(parameterNumber: 11).format()
        ]);  
    }

    //we use this sequence of zwave commands numerous places above, not just in the refresh command,
    // so I have encapsulated them in their own function (rather than calling the refresh() function all over the place)
    // so that the zwave command logging scheme will be consistent -- we should only ever call the logZwaveCommandFromHubToDevice() when we are
    //actually returning a list of events from a command method.
    def getCommandsForRefresh()  {
        return delayBetween([
                zwave.switchBinaryV1.switchBinaryGet().format(), //requests a report of the relay to make sure that it changed (the report is used elsewhere, look for switchBinaryReport()
                //zwave.basicV1.basicGet().format(), //the device does not seem to respond to this command
                zwave.sensorMultilevelV5.sensorMultilevelGet().format(), // requests a report of the anologue input voltage
                (new physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryGet()).format(), //request a report of the (binary) state of the input
                (new physicalgraph.zwave.commands.alarmv1.AlarmGet(alarmType: physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_POWER_MANAGEMENT)).format(), //request a report of the low-voltage alarm state.
                (new physicalgraph.zwave.commands.meterpulsev1.MeterPulseGet()).format() //request a report of the meter pulse count.
            ]);
    }


//}

//{  Z-WAVE LOGGING


    //the following two logZwaveCommand...() functions are pass-through functions -- they will return exactly what is passed to them.  we will wrap all outgoing and incoming zwave commands (in some reasonable format), respectively, in these two functions.
    //These functions accept as arguments exactly the sort of thing that is allowed to be returned from a command function (e.g. off(), on(), refresh(), etc.), namely, a string or an array whose elements are strings (or the type of thing returned by delay())
    //unfortunately, whereas the commands constructed with, for instance, zwave.basicV1.basicGet() produce a meaningful string in response to the format() method, the object returned by zwave.parse(description) in the parse() function behaves differently.
    //therefore, I have resorted to a rather hacky json serialize/deserialize process, so that the hubToDevice commands that we log are of the same type as the deviceToHub commands.
    def logZwaveCommandFromHubToDevice(x) {
        logZwaveCommand(x, "zwaveCommandFromHubToDevice");
        return x;
    }

    def logZwaveCommandFromDeviceToHub(x) {
        logZwaveCommand(x, "zwaveCommandFromDeviceToHub");
        return x;
    }

    //x is expected to be anything that would be a suitable return value for a command method - nameley a list of zwave commands and "delay nnn" directives or a single zwave command.
    //oops -- it is not instances of physicalgraph.zwave.Command that are returned from command functions but rather lists of strings that result from calling the format() method on physicalgraph.zwave.Command objects.
    // fortunately, zwave.parse(x.format()) will return something equivalent to x in the case where x is an instanceof physicalgraph.zwave.Command.  So, if we record the format()'ed strings, we won't be losing any information.
    
    //therefore, x is expected to be a string or a list of strings, and any members that happen to be instances of physicalgraph.zwave.Command will be converted into strings using physicalgraph.zwave.Command's format() method.
    def logZwaveCommand(x, attributeName) {
        try{
            def listOfCommands = 
                (x instanceof java.util.List ? x : [x]).collect{
                    (it instanceof physicalgraph.zwave.Command ? it.format() : it)
                };
            //if we needed to distinguish between strings and zwave commands, we could do " ... instanceof physicalgraph.zwave.Command"
            sendEvent(
                name: attributeName, 
                value: groovy.json.JsonOutput.toJson(listOfCommands),
                displayed: false,
                isStateChange: true //we want to force this event to be recorded, even if the attribute value hasn't changed (which might be the case because we are sending the exact same zwave command for the second time in a row)
            );
            sendEvent(
                name: "zwaveCommand", 
                value: groovy.json.JsonOutput.toJson([direction: attributeName, commands: listOfCommands]), 
                displayed: false,
                isStateChange: true //we want to force this event to be recorded, even if the attribute value hasn't changed (which might be the case because we are sending the exact same zwave command for the second time in a row)
            );
            log.debug(
                (attributeName == "zwaveCommandFromHubToDevice" ? ">>>" : "<<<") + 
                listOfCommands.toString()
            );
        } catch(e) {
            log.debug "exception encountered while running logZwaveCommand: " + e.toString();
        }      
    }    

//}


//{ CONSTANTS
    
    
    private getCommandClassVersionMap() {
        //we will pass this map as the second argument to zwave.parse()
        //this map tells zwave.parse() what version of the various zwave command classes to expect (and controls which version of the zwave classes the zwave.parse() method returns.
        // these values correspond to the version of the various command classes supported by the device.
        return [
            /*0x20*/ (commandClassCodes['BASIC']        ) : 1, 
            /*0x84*/ (commandClassCodes['WAKE_UP']      ) : 1, 
            /*0x30*/ (commandClassCodes['SENSOR_BINARY']) : 1, 
            /*0x70*/ (commandClassCodes['CONFIGURATION']) : 1,
            /*0x71*/ (commandClassCodes['NOTIFICATION'])  : 1,
            /*0x71*/ (commandClassCodes['METER_PULSE'])   : 1,
            /*0x71*/ (commandClassCodes['SWITCH_BINARY'])   : 1,
            /*0x71*/ (commandClassCodes['SENSOR_MULTILEVEL'])   : 5,
            /*0x71*/ (commandClassCodes['ASSOCIATION'])   : 1
        ];
    }
    
    // borrowed from https://github.com/codersaur/SmartThings/blob/master/devices/zwave-tweaker/zwave-tweaker.groovy
    /**
     *  getCommandClassNames()
     *
     *  Returns a map of command class names. Used by toCcNames().
     **/
    private getCommandClassNames() {
        return [
            0x00: 'NO_OPERATION',
            0x20: 'BASIC',
            0x21: 'CONTROLLER_REPLICATION',
            0x22: 'APPLICATION_STATUS',
            0x23: 'ZIP',
            0x24: 'SECURITY_PANEL_MODE',
            0x25: 'SWITCH_BINARY',
            0x26: 'SWITCH_MULTILEVEL',
            0x27: 'SWITCH_ALL',
            0x28: 'SWITCH_TOGGLE_BINARY',
            0x29: 'SWITCH_TOGGLE_MULTILEVEL',
            0x2A: 'CHIMNEY_FAN',
            0x2B: 'SCENE_ACTIVATION',
            0x2C: 'SCENE_ACTUATOR_CONF',
            0x2D: 'SCENE_CONTROLLER_CONF',
            0x2E: 'SECURITY_PANEL_ZONE',
            0x2F: 'SECURITY_PANEL_ZONE_SENSOR',
            0x30: 'SENSOR_BINARY',
            0x31: 'SENSOR_MULTILEVEL',
            0x32: 'METER',
            0x33: 'SWITCH_COLOR',
            0x34: 'NETWORK_MANAGEMENT_INCLUSION',
            0x35: 'METER_PULSE',
            0x36: 'BASIC_TARIFF_INFO',
            0x37: 'HRV_STATUS',
            0x38: 'THERMOSTAT_HEATING',
            0x39: 'HRV_CONTROL',
            0x3A: 'DCP_CONFIG',
            0x3B: 'DCP_MONITOR',
            0x3C: 'METER_TBL_CONFIG',
            0x3D: 'METER_TBL_MONITOR',
            0x3E: 'METER_TBL_PUSH',
            0x3F: 'PREPAYMENT',
            0x40: 'THERMOSTAT_MODE',
            0x41: 'PREPAYMENT_ENCAPSULATION',
            0x42: 'THERMOSTAT_OPERATING_STATE',
            0x43: 'THERMOSTAT_SETPOINT',
            0x44: 'THERMOSTAT_FAN_MODE',
            0x45: 'THERMOSTAT_FAN_STATE',
            0x46: 'CLIMATE_CONTROL_SCHEDULE',
            0x47: 'THERMOSTAT_SETBACK',
            0x48: 'RATE_TBL_CONFIG',
            0x49: 'RATE_TBL_MONITOR',
            0x4A: 'TARIFF_CONFIG',
            0x4B: 'TARIFF_TBL_MONITOR',
            0x4C: 'DOOR_LOCK_LOGGING',
            0x4D: 'NETWORK_MANAGEMENT_BASIC',
            0x4E: 'SCHEDULE_ENTRY_LOCK',
            0x4F: 'ZIP_6LOWPAN',
            0x50: 'BASIC_WINDOW_COVERING',
            0x51: 'MTP_WINDOW_COVERING',
            0x52: 'NETWORK_MANAGEMENT_PROXY',
            0x53: 'SCHEDULE',
            0x54: 'NETWORK_MANAGEMENT_PRIMARY',
            0x55: 'TRANSPORT_SERVICE',
            0x56: 'CRC_16_ENCAP',
            0x57: 'APPLICATION_CAPABILITY',
            0x58: 'ZIP_ND',
            0x59: 'ASSOCIATION_GRP_INFO',
            0x5A: 'DEVICE_RESET_LOCALLY',
            0x5B: 'CENTRAL_SCENE',
            0x5C: 'IP_ASSOCIATION',
            0x5D: 'ANTITHEFT',
            0x5E: 'ZWAVEPLUS_INFO',
            0x5F: 'ZIP_GATEWAY',
            0x60: 'MULTI_CHANNEL',
            0x61: 'ZIP_PORTAL',
            0x62: 'DOOR_LOCK',
            0x63: 'USER_CODE',
            0x64: 'HUMIDITY_CONTROL_SETPOINT',
            0x65: 'DMX',
            0x66: 'BARRIER_OPERATOR',
            0x67: 'NETWORK_MANAGEMENT_INSTALLATION_MAINTENANCE',
            0x68: 'ZIP_NAMING',
            0x69: 'MAILBOX',
            0x6A: 'WINDOW_COVERING',
            0x6B: 'IRRIGATION',
            0x6C: 'SUPERVISION',
            0x6D: 'HUMIDITY_CONTROL_MODE',
            0x6E: 'HUMIDITY_CONTROL_OPERATING_STATE',
            0x6F: 'ENTRY_CONTROL',
            0x70: 'CONFIGURATION',
            0x71: 'NOTIFICATION',
            0x72: 'MANUFACTURER_SPECIFIC',
            0x73: 'POWERLEVEL',
            0x74: 'INCLUSION_CONTROLLER',
            0x75: 'PROTECTION',
            0x76: 'LOCK',
            0x77: 'NODE_NAMING',
            0x7A: 'FIRMWARE_UPDATE_MD',
            0x7B: 'GROUPING_NAME',
            0x7C: 'REMOTE_ASSOCIATION_ACTIVATE',
            0x7D: 'REMOTE_ASSOCIATION',
            0x80: 'BATTERY',
            0x81: 'CLOCK',
            0x82: 'HAIL',
            0x84: 'WAKE_UP',
            0x85: 'ASSOCIATION',
            0x86: 'VERSION',
            0x87: 'INDICATOR',
            0x88: 'PROPRIETARY',
            0x89: 'LANGUAGE',
            0x8A: 'TIME',
            0x8B: 'TIME_PARAMETERS',
            0x8C: 'GEOGRAPHIC_LOCATION',
            0x8E: 'MULTI_CHANNEL_ASSOCIATION',
            0x8F: 'MULTI_CMD',
            0x90: 'ENERGY_PRODUCTION',
            0x91: 'MANUFACTURER_PROPRIETARY',
            0x92: 'SCREEN_MD',
            0x93: 'SCREEN_ATTRIBUTES',
            0x94: 'SIMPLE_AV_CONTROL',
            0x95: 'AV_CONTENT_DIRECTORY_MD',
            0x96: 'AV_RENDERER_STATUS',
            0x97: 'AV_CONTENT_SEARCH_MD',
            0x98: 'SECURITY',
            0x99: 'AV_TAGGING_MD',
            0x9A: 'IP_CONFIGURATION',
            0x9B: 'ASSOCIATION_COMMAND_CONFIGURATION',
            0x9C: 'SENSOR_ALARM',
            0x9D: 'SILENCE_ALARM',
            0x9E: 'SENSOR_CONFIGURATION',
            0x9F: 'SECURITY_2',
            0xEF: 'MARK',
            0xF0: 'NON_INTEROPERABLE'
        ]
    }

    private getCommandClassCodes() {
        //I constructed the below list using groovy as follows:
        // debugMessage += "[" + "\n";
        // getCommandClassNames().collectEntries { key, value -> [value, key] }.sort().each{
            // key, value ->
            // debugMessage += "     " + key.inspect() + ": " + String.format("0x%02X",value) + "," + "\n"
        // }   
        // debugMessage += "]" + "\n";
        
        return [
            'ANTITHEFT': 0x5D,
            'APPLICATION_CAPABILITY': 0x57,
            'APPLICATION_STATUS': 0x22,
            'ASSOCIATION': 0x85,
            'ASSOCIATION_COMMAND_CONFIGURATION': 0x9B,
            'ASSOCIATION_GRP_INFO': 0x59,
            'AV_CONTENT_DIRECTORY_MD': 0x95,
            'AV_CONTENT_SEARCH_MD': 0x97,
            'AV_RENDERER_STATUS': 0x96,
            'AV_TAGGING_MD': 0x99,
            'BARRIER_OPERATOR': 0x66,
            'BASIC': 0x20,
            'BASIC_TARIFF_INFO': 0x36,
            'BASIC_WINDOW_COVERING': 0x50,
            'BATTERY': 0x80,
            'CENTRAL_SCENE': 0x5B,
            'CHIMNEY_FAN': 0x2A,
            'CLIMATE_CONTROL_SCHEDULE': 0x46,
            'CLOCK': 0x81,
            'CONFIGURATION': 0x70,
            'CONTROLLER_REPLICATION': 0x21,
            'CRC_16_ENCAP': 0x56,
            'DCP_CONFIG': 0x3A,
            'DCP_MONITOR': 0x3B,
            'DEVICE_RESET_LOCALLY': 0x5A,
            'DMX': 0x65,
            'DOOR_LOCK': 0x62,
            'DOOR_LOCK_LOGGING': 0x4C,
            'ENERGY_PRODUCTION': 0x90,
            'ENTRY_CONTROL': 0x6F,
            'FIRMWARE_UPDATE_MD': 0x7A,
            'GEOGRAPHIC_LOCATION': 0x8C,
            'GROUPING_NAME': 0x7B,
            'HAIL': 0x82,
            'HRV_CONTROL': 0x39,
            'HRV_STATUS': 0x37,
            'HUMIDITY_CONTROL_MODE': 0x6D,
            'HUMIDITY_CONTROL_OPERATING_STATE': 0x6E,
            'HUMIDITY_CONTROL_SETPOINT': 0x64,
            'INCLUSION_CONTROLLER': 0x74,
            'INDICATOR': 0x87,
            'IP_ASSOCIATION': 0x5C,
            'IP_CONFIGURATION': 0x9A,
            'IRRIGATION': 0x6B,
            'LANGUAGE': 0x89,
            'LOCK': 0x76,
            'MAILBOX': 0x69,
            'MANUFACTURER_PROPRIETARY': 0x91,
            'MANUFACTURER_SPECIFIC': 0x72,
            'MARK': 0xEF,
            'METER': 0x32,
            'METER_PULSE': 0x35,
            'METER_TBL_CONFIG': 0x3C,
            'METER_TBL_MONITOR': 0x3D,
            'METER_TBL_PUSH': 0x3E,
            'MTP_WINDOW_COVERING': 0x51,
            'MULTI_CHANNEL': 0x60,
            'MULTI_CHANNEL_ASSOCIATION': 0x8E,
            'MULTI_CMD': 0x8F,
            'NETWORK_MANAGEMENT_BASIC': 0x4D,
            'NETWORK_MANAGEMENT_INCLUSION': 0x34,
            'NETWORK_MANAGEMENT_INSTALLATION_MAINTENANCE': 0x67,
            'NETWORK_MANAGEMENT_PRIMARY': 0x54,
            'NETWORK_MANAGEMENT_PROXY': 0x52,
            'NODE_NAMING': 0x77,
            'NON_INTEROPERABLE': 0xF0,
            'NOTIFICATION': 0x71,
            'NO_OPERATION': 0x00,
            'POWERLEVEL': 0x73,
            'PREPAYMENT': 0x3F,
            'PREPAYMENT_ENCAPSULATION': 0x41,
            'PROPRIETARY': 0x88,
            'PROTECTION': 0x75,
            'RATE_TBL_CONFIG': 0x48,
            'RATE_TBL_MONITOR': 0x49,
            'REMOTE_ASSOCIATION': 0x7D,
            'REMOTE_ASSOCIATION_ACTIVATE': 0x7C,
            'SCENE_ACTIVATION': 0x2B,
            'SCENE_ACTUATOR_CONF': 0x2C,
            'SCENE_CONTROLLER_CONF': 0x2D,
            'SCHEDULE': 0x53,
            'SCHEDULE_ENTRY_LOCK': 0x4E,
            'SCREEN_ATTRIBUTES': 0x93,
            'SCREEN_MD': 0x92,
            'SECURITY': 0x98,
            'SECURITY_2': 0x9F,
            'SECURITY_PANEL_MODE': 0x24,
            'SECURITY_PANEL_ZONE': 0x2E,
            'SECURITY_PANEL_ZONE_SENSOR': 0x2F,
            'SENSOR_ALARM': 0x9C,
            'SENSOR_BINARY': 0x30,
            'SENSOR_CONFIGURATION': 0x9E,
            'SENSOR_MULTILEVEL': 0x31,
            'SILENCE_ALARM': 0x9D,
            'SIMPLE_AV_CONTROL': 0x94,
            'SUPERVISION': 0x6C,
            'SWITCH_ALL': 0x27,
            'SWITCH_BINARY': 0x25,
            'SWITCH_COLOR': 0x33,
            'SWITCH_MULTILEVEL': 0x26,
            'SWITCH_TOGGLE_BINARY': 0x28,
            'SWITCH_TOGGLE_MULTILEVEL': 0x29,
            'TARIFF_CONFIG': 0x4A,
            'TARIFF_TBL_MONITOR': 0x4B,
            'THERMOSTAT_FAN_MODE': 0x44,
            'THERMOSTAT_FAN_STATE': 0x45,
            'THERMOSTAT_HEATING': 0x38,
            'THERMOSTAT_MODE': 0x40,
            'THERMOSTAT_OPERATING_STATE': 0x42,
            'THERMOSTAT_SETBACK': 0x47,
            'THERMOSTAT_SETPOINT': 0x43,
            'TIME': 0x8A,
            'TIME_PARAMETERS': 0x8B,
            'TRANSPORT_SERVICE': 0x55,
            'USER_CODE': 0x63,
            'VERSION': 0x86,
            'WAKE_UP': 0x84,
            'WINDOW_COVERING': 0x6A,
            'ZIP': 0x23,
            'ZIP_6LOWPAN': 0x4F,
            'ZIP_GATEWAY': 0x5F,
            'ZIP_NAMING': 0x68,
            'ZIP_ND': 0x58,
            'ZIP_PORTAL': 0x61,
            'ZWAVEPLUS_INFO': 0x5E
        ];
    }
//}
