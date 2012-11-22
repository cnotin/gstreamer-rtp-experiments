/* send with different samplerate:
gst-launch-0.10 filesrc location=<<file>> ! 
		mad 		! 
		audioresample 	! audio/x-raw-int,rate=8000 ! 
		audioconvert  	! audio/x-raw-int,channels=2 ! 
		rtpL16pay  	! 
		udpsink host=localhost <<port>>
*/


#include <gst/gst.h>
#include <glib.h>


static gboolean
my_bus_callback (GstBus     *bus,
		 GstMessage *message,
		 gpointer    data)
{
  GMainLoop *loop = data;

  switch (GST_MESSAGE_TYPE (message)) {
    case GST_MESSAGE_ERROR: {
      GError *err;
      gchar *debug;

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
    default:
      /* unhandled message */
      break;
  }

  /* remove message from the queue */
  return TRUE;
}

/* This program implements the following gst-launch commandline:
gst-launch-0.10 filesrc location=DownUnder.mp3 ! 
		mad 		! 
		audioconvert  	! audio/x-raw-int,channels=2 ! 
		rtpL16pay  	! 
		udpsink 	host=localhost port=5000

*/
gint
main (gint   argc,
      gchar *argv[])
{ 
  GMainLoop 	*loop;
  GstElement 	*pipeline, *source, *demuxer, *resamp, *conv, *alaw, *rtp, *sink;
  GstBus 	*bus;  
  gchar 	**exclude_list  = NULL;
  gboolean	g711		= FALSE;
  gboolean      verbose		= FALSE;
  gint	  	port	 	= 5000;
  gchar		*musicFile	= NULL;
  gchar		*ipAddress      = NULL;
  GError 	*err 		= NULL;
  GOptionContext *ctx;

  GOptionEntry 	entries[] = {
    { "location", 'l', 0, G_OPTION_ARG_FILENAME, &musicFile,
      "The mp3 to be streamed", "FILE.mp3" },
    { "host", 'h', 0, G_OPTION_ARG_STRING, &ipAddress,
      "Unicast IP address or multicast group address to stream to", "IP@" },
    { "port", 'p', 0, G_OPTION_ARG_INT, &port,
      "UDP port to stream to (default 5000)", "PORT" },
    { "g711", 'g', 0, G_OPTION_ARG_NONE, &g711,
      "Stream Mono 8 bit 8kHz g711/alaw (default Stereo, L16 44.1kHz)", NULL },
    { "verbose", 'v', 0, G_OPTION_ARG_NONE, &verbose,
      "Print verbose information (default: non-verbose)", NULL },
    { NULL }
  };

  // we must initialise the threading system before using any
  // other GLib funtion, such as g_option_context_new() 
  if (!g_thread_supported ())
    g_thread_init (NULL);

  ctx = g_option_context_new ("- Stream mp3 to unicast/multicast RTP stream (Stereo, L16 44.1kHz - Mono, 8bit 8kHz alaw)");
  g_option_context_add_main_entries (ctx, entries, NULL);
  g_option_context_add_group (ctx, gst_init_get_option_group ());
  if (!g_option_context_parse (ctx, &argc, &argv, &err)) {
    g_print ("Failed to initialize: %s\n", err->message);
    g_error_free (err);
    return 1;
  }

  g_option_context_free(ctx);
  if (!musicFile || !ipAddress)
  {
    printf("location or host option not given. Use --help to see the options\n");
    g_free(musicFile);
    g_free(ipAddress);
    return -1;
  }

  /* init GStreamer */
  gst_init (&argc, &argv);

  loop = g_main_loop_new (NULL, FALSE);

  /* Create gstreamer elements */
  pipeline = 	gst_pipeline_new (NULL);
  source   = 	gst_element_factory_make ("filesrc", NULL);
  demuxer  = 	gst_element_factory_make ("mad", NULL);
  resamp   =	gst_element_factory_make ("audioresample", NULL);
  conv     = 	gst_element_factory_make ("audioconvert", NULL);
  sink     = 	gst_element_factory_make ("udpsink", NULL);

  if (g711) {
    rtp	   = gst_element_factory_make ("rtppcmapay", NULL);
    alaw   = gst_element_factory_make ("alawenc", NULL);
    if (!alaw) {
      g_printerr ("One element could not be created.\n");
      return -1;
    }
  } else {
    rtp	   = gst_element_factory_make ("rtpL16pay", NULL);
  }

  if (!pipeline || !source || !demuxer || !resamp || !conv || !rtp || !sink) 
  {
    g_printerr ("One element could not be created. Exiting.\n");
    return -1;
  }

  if (verbose) {/* get verbose information during start of the pipeline */
    g_signal_connect (	pipeline, 
			"deep-notify",
        		G_CALLBACK (gst_object_default_deep_notify), 
			exclude_list);
  }
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
  gst_bus_add_watch (bus, my_bus_callback, loop);
  gst_object_unref (bus);

  /* link */
  
  if (g711) {
    gst_bin_add_many (GST_BIN (pipeline), source, demuxer, resamp, conv, alaw, rtp, sink, NULL);
    if (!gst_element_link_many (source, demuxer, resamp, conv, alaw, rtp, sink, NULL)) 
    {
      g_warning ("Failed to link elements!");
    }
  } else {
    gst_bin_add_many (GST_BIN (pipeline), source, demuxer, resamp, conv, rtp, sink, NULL);
    if (!gst_element_link_many (source, demuxer, resamp, conv, rtp, sink, NULL)) 
    {
      g_warning ("Failed to link elements!");
    }
  }
  /* set the source audio file */
  g_object_set (source, "location", musicFile, NULL);

  /* set RTP parameters */
  g_object_set (sink, "host", ipAddress, "port", port, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* now run */
  g_main_loop_run (loop);

  /* also clean up */
  g_free(musicFile);
  g_free(ipAddress);
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));

  return 0;
}
