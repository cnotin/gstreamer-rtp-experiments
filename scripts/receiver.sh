#!/bin/sh

#gst-launch --gst-debug=2 gstrtpbin name=rtpbin \
#    udpsrc port=5002 caps="application/x-rtp" ! rtpbin.recv_rtp_sink_0 \
#        rtpbin. ! fakesink \
#    udpsrc port=5003 caps="application/x-rtcp" ! rtpbin.recv_rtcp_sink_0 \
#        rtpbin.send_rtcp_src_0 ! udpsink port=5007 sync=false async=false
GST_DEBUG="rtpbin:4, udpsrc:4, 2" gst-launch -v gstrtpbin name=rtpbin \
    udpsrc port=5002 caps="application/x-rtp, media=(string)audio, clock-rate=(int)8000, encoding-name=(string)PCMU, channels=(int)1, payload=(int)0" ! rtpbin.recv_rtp_sink_0 \
        rtpbin. ! rtppcmudepay ! mulawdec ! audioconvert ! autoaudiosink \
    udpsrc port=5003 ! rtpbin.recv_rtcp_sink_0
#    rtpbin.send_rtcp_src_0 ! udpsink port=5007 sync=false async=false
