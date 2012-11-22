
#include <gst/gst.h>
#include <glib.h>
#include <stdio.h>

// Forward function declarations
static gboolean
my_bus_callback (GstBus     *bus,
		 GstMessage *message,
		 gpointer    data);
static void
on_pad_removed (GstPad     *element,  	// the pad that is unlinked
                GstPad     *pad,	// the peer pad (in the bin)
                gpointer   data);
static void
on_pad_added (GstElement  *element,
              GstPad      *pad,
              gpointer    data);


GstElement	*pipeline;
  gboolean	g711		= FALSE;


static gboolean
my_bus_callback (GstBus     *bus,
		 GstMessage *message,
		 gpointer    data)
{
  GMainLoop 		*loop = data;
  GstObject 	 	*src_obj;
  const GstStructure 	*s;
  guint32 		seqnum;

  switch (GST_MESSAGE_TYPE (message)) {
    case GST_MESSAGE_ERROR: {
      GError *err;
      gchar  *debug;

      gst_message_parse_error (message, &err, &debug);
      g_print ("Error: %s\n", err->message);
      g_error_free (err);
      g_free (debug);

      g_main_loop_quit (loop);
      break;
    }

    case GST_MESSAGE_EOS:
      /* end-of-stream */
      g_main_loop_quit (loop);
      break;

    case GST_MESSAGE_ELEMENT: {
/*
      seqnum 	= gst_message_get_seqnum (message);
      s 	= gst_message_get_structure (message);
      src_obj 	= GST_MESSAGE_SRC (message);

      if (GST_IS_ELEMENT (src_obj)) {
        g_print (("Got message #%u from element \"%s\" (%s): "),
            (guint) seqnum, GST_ELEMENT_NAME (src_obj),
            GST_MESSAGE_TYPE_NAME (message));
      } else if (GST_IS_PAD (src_obj)) {
        g_print (("Got message #%u from pad \"%s:%s\" (%s): "),
            (guint) seqnum, GST_DEBUG_PAD_NAME (src_obj),
            GST_MESSAGE_TYPE_NAME (message));
      } else if (GST_IS_OBJECT (src_obj)) {
        g_print (("Got message #%u from object \"%s\" (%s): "),
            (guint) seqnum, GST_OBJECT_NAME (src_obj),
            GST_MESSAGE_TYPE_NAME (message));
      } else {
        g_print (("Got message #%u (%s): "), (guint) seqnum,
            GST_MESSAGE_TYPE_NAME (message));
      }

      if (s) {
        gchar *sstr;

        sstr = gst_structure_to_string (s);
        g_print ("%s\n", sstr);
        g_free (sstr);
      } else {
        g_print ("no message details\n");
      }
*/
    }
    default:
      /* unhandled message */
      break;
  }

  /* remove message from the queue */
  return TRUE;
}

/* This program implements the following gst-launch commandline:
gst-launch-0.10 -v 	udpsrc multicast-group=224.1.1.1 port=5000 caps="application/x-rtp,media=(string)audio, clock-rate=(int)44100, channels=(int)2, payload=(int)96" ! 
			gstrtpjitterbuffer do-lost=false ! 
			rtpL16depay ! 
			audioconvert ! 
			audioresample !
			rganalysis message-true !	//Optional
			alsasink
*/


static void
on_pad_added (GstElement  *element,
              GstPad      *pad,
              gpointer    data)
{
  GstElement 	*mybin,   *rtp, *conv, *resample;
  GstElement	*alaw = NULL;
  GstPad 	*sinkpad, *mypad;
  GstElement 	*liveadder = (GstElement *) data;

  mybin	     = 	gst_bin_new(NULL);
  if (g711) {
    rtp	 = gst_element_factory_make ("rtppcmadepay", NULL);
    alaw = gst_element_factory_make("alawdec", NULL); 
    if (!alaw) {
      g_printerr ("on_pad_added; One element could not be created.\n");
      return;
    } else {
      if (!gst_bin_add (GST_BIN (mybin), alaw)) {
        g_printerr ("on_pad_added; One element could not be added to pipeline.\n");
        return;
      }
    }
  } else {
    rtp	     =	gst_element_factory_make ("rtpL16depay", NULL);
  }

  //  rganalyse  = 	gst_element_factory_make ("rganalysis",NULL);
  conv       = 	gst_element_factory_make ("audioconvert", NULL);
  resample   = 	gst_element_factory_make ("audioresample",NULL);

  if (!rtp || !conv || !resample /*!rganalyse ||*/ || !mybin) {
    g_printerr ("on_pad_added; One element could not be created.\n");
    return;
  }
  gst_bin_add_many (GST_BIN (mybin), rtp, conv, resample, /*rganalyse,*/ NULL);
  if (!gst_bin_add(GST_BIN (pipeline), mybin))
  {
    g_printerr ("on_pad_added; mybin can not be added to pipeline.\n");
    return;
  }
  //  g_object_set (G_OBJECT(rganalyse), "message", TRUE, NULL);

  if (g711) {
    if (!gst_element_link_many (rtp, alaw, conv, resample, /*rganalyse,*/ NULL)) {
      g_warning ("on_pad_added: Failed to link elements!");
      return;
    }
  } else {
    if (!gst_element_link_many (rtp, conv, resample, /*rganalyse,*/ NULL)) {
      g_warning ("on_pad_added: Failed to link elements!");
      return;
    }
  }

  // create sink and source ghostpads on mybin
  mypad = gst_element_get_static_pad (rtp, "sink");
  if (!mypad) {
    g_warning ("Failed to get sink pad from rtp!");
    return;
  } else {
    if (!gst_element_add_pad (mybin, gst_ghost_pad_new ("sink", mypad))) {
      g_warning ("Failed to add ghost sinkpad to bin!");
    }
    gst_object_unref(mypad);
  }

  mypad = gst_element_get_static_pad (resample, "src");
  if (!mypad) {
    g_warning ("Failed to get source pad from resample!");
    return;
  } else {
    if (!gst_element_add_pad (mybin, gst_ghost_pad_new ("src", mypad))) {
      g_warning ("Failed to add ghost sourcepad to bin!");
    }
    gst_object_unref(mypad);
  }
  
  //link bin to the liveadder
  if (!gst_element_link(mybin, liveadder)) {
    g_warning ("Failed to link bin to liveadder!");
    return;
  }

  // link dynamically created rtpbin pad to sinkpad of mybin
  sinkpad = gst_element_get_static_pad (mybin, "sink");	// get the sink pad from bin
  if (!gst_pad_is_linked (sinkpad)) {
    if (gst_pad_link (pad, sinkpad) != GST_PAD_LINK_OK) {
      g_error ("Failed to link pads!");
    }
  } else {     
    g_warning ("gstrtppad is already linked!\n");
  }
  gst_object_unref (sinkpad);

  // When the RTP stream of this new pad stops, this pad will be unlinked. At that moment the parent of the peer pad (this is the bin we just connected) can be removed from the pipeline. 
  g_signal_connect (pad, "unlinked", G_CALLBACK (on_pad_removed), NULL);  

  // Set new elements to PAUSED
  gst_element_set_state (mybin, GST_STATE_PAUSED);
}

static void
on_pad_removed (GstPad     *element,  	// the pad that is unlinked
                GstPad     *pad,	// the peer pad (in the bin)
                gpointer   data)
{
  GstElement	*mybin;

  // rtpbin pad is removed. Remove the "mybin" connected between the rtpbin and the liveadder.
  // Find the peer pad of the given rtpbin pad

  mybin = gst_pad_get_parent_element(pad);
  if (!mybin) {
    g_warning("on_pad_removed, parent is not found\n");
  } else {
    gst_element_set_state (mybin, GST_STATE_NULL);    
    if (!gst_bin_remove(GST_BIN(pipeline), mybin)) {
      g_warning("on_pad_removed, can not remove mybin from pipeline\n");
    }
  }
  gst_object_unref(mybin);
}

gint
main (gint   argc,
      gchar *argv[])
{ 
  GMainLoop 	*loop;
  GstElement 	*source, *rtpbin, *liveadder, *sink;
  GstBus 	*bus;  
  GstCaps 	*caps;
  gchar 	**exclude_list  = NULL;
  gboolean      verbose		= FALSE;
  gint	  	port	 	= 5000;
  gchar		*ipAddress      = NULL;
  GError 	*err 		= NULL;
  GOptionContext *ctx;

  GOptionEntry 	entries[] = {
    { "multicast", 'm', 0, G_OPTION_ARG_STRING, &ipAddress,
      "Multicast group address to receive from (default: localhost)", "IP@" },
    { "port", 'p', 0, G_OPTION_ARG_INT, &port,
      "UDP port to receive from (default 5000)", "PORT" },
    { "g711", 'g', 0, G_OPTION_ARG_NONE, &g711,
      "Receive Mono 8 bit 8kHz g711/alaw (default: Stereo, L16 44.1kHz)", NULL },
    { "verbose", 'v', 0, G_OPTION_ARG_NONE, &verbose,
      "Print verbose information (default non-verbose)", NULL },
    { NULL }
  };

  // we must initialise the threading system before using any
  // other GLib funtion, such as g_option_context_new() 
  if (!g_thread_supported ())
    g_thread_init (NULL);

  ctx = g_option_context_new ("- Dynamically mix multiple unicast/multicast RTP streams (Stereo, L16 44.1kHz / Mono, 8bit 8kHz alaw)");
  g_option_context_add_main_entries (ctx, entries, NULL);
  g_option_context_add_group (ctx, gst_init_get_option_group ());
  if (!g_option_context_parse (ctx, &argc, &argv, &err)) {
    g_print ("Failed to initialize: %s\n", err->message);
    g_error_free (err);
    return 1;
  }

  g_option_context_free(ctx);

  // Init GStreamer
  gst_init (&argc, &argv);
  loop = g_main_loop_new (NULL, FALSE);

  // Create gstreamer elements for first and last part of the pipeline
  pipeline   = 	gst_pipeline_new 	 (NULL);
  source     = 	gst_element_factory_make ("udpsrc", NULL);
  rtpbin     = 	gst_element_factory_make ("gstrtpbin", NULL);
  liveadder  = 	gst_element_factory_make ("liveadder",NULL);
  sink       = 	gst_element_factory_make ("alsasink", NULL);

  if (!pipeline || !source || !rtpbin || !liveadder || !sink) 
  {
    g_printerr ("One element could not be created. Exiting.\n");
    return -1;
  }

  if (verbose) {/* get verbose information during start of the pipeline */  // Set communications with bus and verbose information during start of the pipeline
    g_signal_connect (	pipeline, 
			"deep-notify",
        		G_CALLBACK (gst_object_default_deep_notify), 
			exclude_list);
  }
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  gst_bus_add_watch (bus, my_bus_callback, loop);
  gst_object_unref (bus);

  /* set media type of the udp source */
  if (g711) {
    caps = gst_caps_new_simple ("application/x-rtp",
				"media",	G_TYPE_STRING, 	"audio",
				"clock-rate",	G_TYPE_INT, 	8000,		//<<==== this must be clock-rate not rate !!!!
				"channels", 	G_TYPE_INT, 	1,
				"payload", 	G_TYPE_INT, 	8,
				"encoding-name",G_TYPE_STRING,  "PCMA", 
				NULL);
  } else {
    caps = gst_caps_new_simple ("application/x-rtp",
				"media",	G_TYPE_STRING, 	"audio",
				"clock-rate",	G_TYPE_INT, 	44100,		//<<==== this must be clock-rate not rate !!!!
				"channels", 	G_TYPE_INT, 	2,
				"payload", 	G_TYPE_INT, 	96, 
				NULL);
  }

  g_object_set (G_OBJECT (source), "caps", caps, NULL);
  gst_caps_unref (caps);

  gst_bin_add_many (GST_BIN (pipeline), source, rtpbin, /*rganalyse,*/ liveadder, sink, NULL);

  // Link first and last part of the pipeline
  if (!gst_element_link_many (source, rtpbin, NULL))  {
    g_warning ("Failed to link source to rtpbin!");
  }

  if (!gst_element_link_many (liveadder, sink, NULL))  {
    g_warning ("Failed to link liveadder to alsasink!");
  }

  g_signal_connect (rtpbin, "pad-added", G_CALLBACK (on_pad_added), liveadder);
  // signal handler to handle pad removed is set in the on_pad_added function

  /* set the attributes of the elements */
  if (ipAddress) {
    g_object_set (G_OBJECT(source), "multicast-group", ipAddress/*argv[2]*/, NULL);
    g_free(ipAddress);
  }
  g_object_set (G_OBJECT(source), "port", port /*atoi(argv[1])*/, NULL);
  g_object_set (G_OBJECT(rtpbin), "autoremove", TRUE, NULL);
  g_object_set (G_OBJECT(sink), "sync", FALSE, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* now run */
  g_main_loop_run (loop);

  /* also clean up */
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));

  return 0;
}
