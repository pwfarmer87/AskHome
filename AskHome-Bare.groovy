/**
 *  Alexa and Lambda - Perform an operation on a device as requested by Alexa
 *
 *  Copyright 2016 Keith DeLong (n8xd)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  5/4/16  KHD (n8xd) added OAUTH token connect inside app
 *  4/29/16 KHD (n8xd) adjusted some response wording for motion
 *  4/26/16 KHD (n8xd) call it anything, control anything, devices, rooms, etc
 *  4/22/16 KHD (n8xd) recoded centralCommand with capability subroutines
 *  4/20/16 KHD (n8xd) test jig for processing device and op commands sent through Alexa/Lambda
 */
 
definition(
    name: "AskHome",
    namespace: "n8xd",
    author: "n8xd",
    description: "Do what Alexa tells us to do",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/n8xd/AskHome/master/askhome108.png",
    iconX2Url: "https://raw.githubusercontent.com/n8xd/AskHome/master/askhome512.png")


preferences {
   page(name: "connectDevPage")
   }

// Use inputs to attach smartthings devices to this app
def connectDevPage() {
   dynamicPage(name: "connectDevPage", title:"Connect Devices",  install: true, uninstall: true ) {
      section(title: "Select Devices") {
        input "brlight", "capability.switch", title: "Select the Bedroom Light", required: true, multiple:false
        // Add your inputs here
      }
      if (!state.tok) { try { state.tok = createAccessToken() } catch (error) { state.tok = null } }
      section(title: "Show the OAUTH ID/Token Pair") {
        paragraph "   var STappID = '${app.id}';\n   var STtoken = '${state.tok}';\n"
      }
   }
}


mappings { path("/:noun/:operator/:operand/:inquiz"){ action: [GET: "centralCommand"] } }

def installed() {}
def updated() {}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
// Central Command
//
// take a device and operation the user has spoken to Alexa, 
//     perform the operation on the device and confirm with words.
//////////////////////////////////////////////////////////////////////////////////////////////////////////

def centralCommand() {
        log.debug params

	    def noun = params.noun
        def op  = params.operator 
        def opa = params.operand   
        def inq = params.inquiz    
        
        log.debug "Central Command  ${noun} ${op} ${opa} ${inq}"
        
        state.talk2me = ""    
        
        // adjust for the english language, if the user uses inquiry words, then switch things to status in select cases
        // ask home if something is on  -- is really a status check, not command to change something
        // ask home how hot is the basement -- is really a temperature check
        // so if there is an inquisitor, then check the status instead of doing a command.
       
        if (op == "none") { op = "status" }                      //if there is no op, status request
        if (["done","finished"].contains(op)) { op = "status" }  //with or without inquisitor these are status
        if (inq != "none") {                                     //with an inquisitor these are status
            if (["on","off","open","closed","locked","unlocked","about","running"].contains(op)) { op = "status" }
            else if (["hot","cold","warm","cool"].contains(op)) { op = "temperature" }
        }

        // nouns - persons places and things, based on which noun you want to work with, and which operation
        // you want to perform...take some actions on smartthings devices and their capabilities.
        // make sure you put your nouns in the Alexa Developer part under LIST_OF_NOUNS, same for any new
        // operations you make up...remember you can say "pop" and "cap" for open and closed if you
        // put them in the LIST_OF_OPERATORS and list it in the "op" cases below.
        // you can also use on and off with open and close devices...turn the water off (instead of open the city water valve)
        // simply by including them in the op cases.  Adjust the capability to recognize the word, or make a new capability
        // that does the same thing, with the alternate operators and call it, instead.


        switch (noun) {
            case "bedroom light"       :  switch(op) {       // simple on and off
                                            case "on"        :
                                            case "off"       : 
                                            case "status"    : switchResponse(brlight,noun,op); break
                                            default          : defaultResponseUnkOp(noun,op)
                                          }
                                          break
                                          
            // Add your case statements here
            
             case "none"                :  defaultResponseWhat()
                                           break
                                          
            default                     :  defaultResponseUnkNoun(noun,op)
      }
      
      return ["talk2me" : state.talk2me]
}



//////////////////////////////////////////////////////////////////////////////////////////////////////////
//capability responses - DO and Report
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////

def defaultResponseWhat()
{
      state.talk2me = state.talk2me + "Ask me about something, or to do something with something.   " 
}

// defaultResponse Unknown Device
def defaultResponseUnkNoun(noun, op)
{
      state.talk2me = state.talk2me + "I can't find an person, place, or thing called ${noun} in the smart app.  " 
}


// defaultResponse Unknown Operator for device
def defaultResponseUnkOp(noun, op)
{
      state.talk2me = state.talk2me + "I haven't been told how to do ${op} with ${noun} yet.  "
}


//capability.switch  ["on", "off"]
def switchResponse(handle, noun, op)
{
      def arg = handle.currentValue("switch")                         //value before change 
           if (op == "on") { handle.on(); arg = "turning " + op;}     //switch flips slow in state, so tell them we did Op
      else if (op == "off") { handle.off(); arg = "turning " +op; }   // ...or it will report what it was, not what we want
      else if (op == "status") { }                                    // dont report Op, report the real currentState
      state.talk2me = state.talk2me + "The ${noun} is ${arg}.  "      // talk2me : switch is on (or off) 
}


//capability.presenceSensor ["present","not present"]
def presenceSensorResponse(handle, noun, op)
{
      def arg = handle.currentValue("presence")                       // lookup the current presence status
      state.talk2me = state.talk2me + "The ${noun} is ${arg}.  "      // talk2me : sensor is present (or not present)
}


//capability.valve   ["open","closed"]
def valveResponse(handle, noun, op)
{
      def arg = handle.currentValue("contact")                         //value before change 
           if (op == "open") { handle.open(); arg = op + "ing";}       //switch flips slow in state, so tell them we did Op
      else if (op == "close") { handle.close(); arg = op + "ing"; }    // ...or it will report what it was, not what we want
      else if (op == "status") { }                                     // dont report Op, report the real currentState
      state.talk2me = state.talk2me + "The ${noun} is ${arg}.  "       // talk2me : valve is open or closed  
}


//capability.valve   ["on","off"]                                      // example to mix it up (copied valve, replaced open/close
def valveResponseOnOff(handle, noun, op)                               // with on/off
{
      def arg = handle.currentValue("contact")                         //value before change 
           if (op == "on") { handle.open(); arg = "turning " + op;}    //switch flips slow in state, so tell them we did Op
      else if (op == "off") { handle.close(); arg = "turning " + op; } // ...or it will report what it was, not what we want
      else if (op == "status") {                                       // dont report Op, report the real currentState
          if (arg == "open") { arg = "on" }                            // fix up a status report so it uses on / off
          else if (arg == "closed") { arg = "off" }
      }
      state.talk2me = state.talk2me + "The ${noun} is ${arg}.  "       // talk2me : valve is open or closed  
}


//capability.battery          ["%"]
def batteryResponse(handle, noun, op)  
{
      def arg = handle.currentValue("battery")                                         // lookup the current battery status
      state.talk2me = state.talk2me + "The ${noun} battery is at ${arg} percent.  "    // talk2me : battery is at xx% percent
}

//capability.battery ALL
def batteryResponseMulti(handle, noun, op)                                             // handle All battery check differently
{
          state.talk2me = state.talk2me + "These batteries are low:  "
          handle.each {
             def arg = it.currentValue("battery")
             if (arg) {
                 if (arg.toInteger() < 85) {
                     state.talk2me = state.talk2me + "${it} at ${arg}%.  "  
                 }
             }
          }
}

//capability.smokeDetector   ["clear", "detected", "tested"]
def smokeDetectorResponse(handle, noun, op)
{
      def arg = handle.currentValue("smoke")                            // lookup the current smoke detector status
      if (!arg) {arg == "clear"}                                  // default if unreported and value doesn't exist yet
      state.talk2me = state.talk2me + "The ${noun} smoke is ${arg}.  "  // talk2me : detector is clear, detected
}


//capability.carbonMonoxideDetector    ["clear", "detected", "tested"]   **FIX** Returns null for currentValue
def CODetectorResponse(handle, noun, op)
{
      def arg = handle.currentValue("carbonMonoxide")                   // lookup the CO status
      if (!arg) {arg = "clear"}                                  // default if unreported and value doesn't exist yet
      state.talk2me = state.talk2me + "The ${noun} carbon monoxide is ${arg}.  "    // talk2me : detector is clear, detected
}


//capability.contactSensor   ["open", "closed"]
def contactSensorResponse(handle, noun, op)
{
      def arg = handle.currentValue("contact")                                      // lookup the current contact status
      state.talk2me = state.talk2me + "The ${noun} is ${arg}.  "                    // talk2me : contact is open or closed
}


//capability.threeAxis 
def threeAxisResponse(handle, noun, op)
{
      def arg = handle.currentValue("threeAxis")                                    // lookup the current threeAxis status
      def pos = getOrientation(handle,arg)                                          // enumerate the position (optional)
      state.talk2me = state.talk2me + "The ${noun} is in position number ${pos}.  "
      state.talk2me = state.talk2me + "The ${noun} is oriented where X equals ${arg.x}, Y equals ${arg.y}, and z equals ${arg.z}.  "
}


//capability.lock  ["locked", "unlocked"]
def lockResponse(handle, noun, op, opa)
{ 
      def arg = handle.currentValue("lock")                           // lookup the current lock
      if (op == "lock") { handle.lock(); arg = op+"ed";}              //switch flips slow in state, so tell them we did Op
      else if (op == "unlock") { 
           if (opa == passwd) { handle.unlock(); arg = op+"ed"; }
           else if (opa == "none")  // and passwd did not equal none
             { state.talk2me = state.talk2me + "Repeat the unlock command but include your unlock password at the end.  " }
           else { state.talk2me = state.talk2me + "Your password did not match.  "}
      }     
      else if (op == "status") { }                                   // dont report Op, report the real currentState
      state.talk2me = state.talk2me + "The ${noun} is ${arg}.  "     // talk2me : lock is open or closed
}


//capability.temperatureMeasurement ["degrees"] 
def temperatureMeasurementResponse(handle, noun, op)
{                                  
      def arg = handle.currentValue("temperature")
      state.talk2me = state.talk2me + "The ${noun} temperature is ${arg} degrees.  "
}


//capability.motionSensor ["active", "inactive"]
def motionSensorResponse(handle, noun, op)
{
      def arg = handle.currentValue("motion")
      if (arg == "active") { arg = "motion" }
      else if ( arg == "inactive") { arg = "no motion" }
     
      state.talk2me = state.talk2me + "There is ${arg} in the ${noun}.  "
}

def motionSensorBoilingWaterResponse(handle,noun,op)
{
      def arg = handle.currentValue("motion")
      def arg2 = ""
      
      if (arg == "active") { arg = "motion"; arg2 = "boiling"; }
      else if ( arg == "inactive") { arg = "no motion"; arg2 = "not boiling"; }
      state.talk2me = state.talk2me + "Their is ${arg}.  The water is ${arg2}."
}


//capability.waterSensor ["wet","dry"]   //  **FIX** returns null for currentValue
def waterSensorResponse(handle, noun, op)
{
      def arg = handle.currentValue("water")
      if (!arg) { arg = "dry" }     // default if value has not been reported yet
      state.talk2me = state.talk2me + "The ${noun} is ${arg}.  "
}


//capability.polling ["poll", "update"]
def pollingResponse(handle, noun, op)
{
      handle.poll()
      state.talk2me = state.talk2me + "The ${noun} has been ${op}d.  "
}


//capability.colorControl
def colorControlResponse(handle, noun, op, opa)
{
     if (op == "go") { 
          def hueval = colorWordtoHue(opa)
          def satval = 0
          
          if (opa == "white") {satval = 20} else {satval = 100}
          
          if (hue == -2) {state.talk2me = state.talk2me + "I don't know that color yet.  " } 
          else if (hue == -1) { handle.off()}
          else {
               def map = [switch: "on", hue: hueval, saturation: satval, level: level as Integer ?: 100]
               handle.setColor(map)
               handle.on()
          }         
     }
     state.talk2me = state.talk2me + "${op} on ${noun} to ${opa}.  "
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
// Noun Responses -- no direct capability action, but may use capabilities
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////

def clothsNounResponse(handlew,handled) 
{
     def wnot = "not"
     def dnot = "not"
     if (handlew.currentValue("power") > 0) { wnot = "" }
     if (handled.currentValue("power") > 0) { dnot = "" }
     state.talk2me = state.talk2me + "The washer is ${wnot} running, and the dryer is ${dnot} running.  "
}


def stoveNounResponse(handlestovetemp, handleroomtemp, handlestovemotion)
{
     def stemp = handlestovetemp.currentValue("temperature")
     def rtemp = handleroomtemp.currentValue("temperature")
     def diftemp = stemp - rtemp
     def hc = "hotter"
     if (diftemp < 0) {hc = "cooler"; diftemp = diftemp.abs()}
     
     motionSensorResponse(handlestovemotion,"stove","status")
     state.talk2me = state.talk2me + "Above the stove is ${stemp} degrees, and that is ${diftemp} degrees ${hc} than the room temperature of ${rtemp} degrees."
}

def poolNounResponse(gatehandle,stereohandle)
{
     contactSensorResponse(gatehandle,"pool gate","status")
     switchResponse(stereohandle,"pool stereo","status")
     state.talk2me = state.talk2me + "The pool scene lights are awating installation.  "
     state.talk2me = state.talk2me + "The replacment pump and filter are awaiting installation.  "
     state.talk2me = state.talk2me + "The special water proof Echo swim suit is on order from Amazon so I can join you in the pool. "
}

def keithNounResponse()
{
     state.talk2me = state.talk2me + "Keith created this Alexa to Smart Things connection.  With it, you can assign "
     state.talk2me = state.talk2me + "nouns and operations to control and report on Smart Things.  This is different "
     state.talk2me = state.talk2me + "because it is not centered around devices, but centers around people, places, or things. "
     state.talk2me = state.talk2me + "Keith is not responsible if it enables Alexa's self destruct code by accident.  " 
     state.talk2me = state.talk2me + "This program is available under the Apache 2.0 license...so hack it to fit your needs.  "
     state.talk2me = state.talk2me + "Copyright Keith DeLong 2016, all rights reserved."
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
// special routine to calculate color hue from a color word  for capability.colorControl
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////

private colorWordtoHue(colorWord)
{
     def hueColor = -2;    // -2 not found, -1 black, 0-100 hue
     switch (colorWord) { 
        case "black" : huecolor = -1; break
        case "white" : hueColor = 52; break
        case "blue"  : hueColor = 70; break
        case "green" : hueColor = 39; break
        case "yellow": hueColor = 17; break
        case "maize" : hueColor = 17; break
        case "orange": hueColor = 10; break
        case "purple": hueColor = 75; break
        case "Pink"  : hueColor = 83; break
        case "Red"   : hueColor = 100; break
     }
     return hueColor
} 



//////////////////////////////////////////////////////////////////////////////////////////////////////////
// special routine to enumerate a 3axis position 
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////


private getOrientation(handle, xyz) {

    def value = handle.currentValue("threeAxis")
    log.debug value
    def orientation = 0
    
    // This is the coordinates for a 5 point star
    if (isNear(value.y, 0) && isNear(value.z,1000)) { orientation = 1 }
    else if (isNear(value.y, 1000) && isNear(value.z,350)) { orientation = 2 }
    else if (isNear(value.y, 600) && isNear(value.z,-820)) { orientation = 3 }
    else if (isNear(value.y, -580) && isNear(value.z,-850)) { orientation = 4 }
    else if (isNear(value.y, -1000) && isNear(value.z,300)) { orientation = 5 }
    else if (isNear(value.x, 1000) && isNear(value.y,0) && isNear(value.z,0)) { orientation = 6 }
    
    log.debug "${value.x}, ${value.y}, ${value.z} = orientation ${orientation}"   
    orientation
}

private isNear(w, d)
{
	def tol = 200
    return  Math.abs((w - d)) < tol      
 }

