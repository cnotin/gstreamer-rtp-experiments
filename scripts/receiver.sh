#!/bin/sh

#gst-launch --gst-debug=2 gstrtpbin name=rtpbin \
#    udpsrc port=5002 caps="application/x-rtp" ! rtpbin.recv_rtp_sink_0 \
#        rtpbin. ! fakesink \
#    udpsrc port=5003 caps="application/x-rtcp" ! rtpbin.recv_rtcp_sink_0 \
#        rtpbin.send_rtcp_src_0 ! udpsink port=5007 sync=false async=false
        
gst-launch --gst-debug=2 --gst-debug-no-color gstrtpbin name=rtpbin \
    udpsrc port=5002 caps="application/x-rtp, media=(string)audio, clock-rate=(int)44100, encoding-name=(string)L16, encoding-params=(string)1, channels=(int)1, payload=(int)96" ! rtpbin.recv_rtp_sink_0 \
        rtpbin. ! rtpL16depay ! audioconvert ! autoaudiosink \
    udpsrc port=5003 ! rtpbin.recv_rtcp_sink_0
#    rtpbin.send_rtcp_src_0 ! udpsink port=5007 sync=false async=false
