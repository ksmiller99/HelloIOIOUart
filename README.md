# HelloIOIOServiceIPC
Testing 2-way Inter-Process-Communication with IOIOService
The HelloIoIoService example is what I started with, and then added a button on the Main to start and stop the blinking, 
and a second activity that is basically a copy of the main. I also added Messengers to both activities and a message 
handler in the IOIO service. The IOIO service broadcasts Intents about the status changes of the IOIO when it connects 
or disconnects. The main activity starts the service and then binds to it to send messages. When the second activity starts, 
it binds and sends a message requesting the LED status (blinking or off) and updates the button label based on the message reply.
The activities unbind from the IOIOService in onDestroy().
