# carGateway

carGateway is an open source gateway to send data to a cloud server. It is built upon the Eclipse Kura framework with OSGi and uses test data from OpenXC. It is possible to send the plugin to a Kura installation on a Raspberry Pi to emulate a car. Test data from Downtown, Crosstown are included in this project.

### Tech

carGateway uses a number of open source projects to work properly:

* [Eclipse Kura] - OSGi-based Application Framework for M2M Service Gateways (Eclipse Public License - v 1.0)
* [OpenXC data] - Vehicle Trace Files from OpenXC (Creative Commons Attribution International 4.0 License)

And of course carGateway itself is open source on GitHub.

### Installation

Prerequisites on Raspberry Pi:
* Raspbian
* Kura for Raspberry Pi (Tutorial: http://eclipse.github.io/kura/doc/raspberry-pi-quick-start.html)
* Open port 1450 (for access via Eclipse)

Prerequisites on development environment:
* Eclipse
* Setting up Kura (Tutorial: http://eclipse.github.io/kura/doc/kura-setup.html; Important: Installation of mToolkit)
* Possibility to connect via SSH to Raspberry Pi

License
----

Apache 2.0



   [Eclipse Kura]: <https://github.com/eclipse/kura>
   [OpenXC data]: <http://openxcplatform.com/resources/traces.html>
