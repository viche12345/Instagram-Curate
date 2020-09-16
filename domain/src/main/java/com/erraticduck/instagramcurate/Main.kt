package com.erraticduck.instagramcurate

import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.domain.Session
import com.erraticduck.instagramcurate.sync.InstagramExecutor
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter

internal object Main {

    private val header = """
        <!doctype html>
        <html lang="en">
        <head>
        <!-- Meta tags for Bootstrap -->
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

        <!-- Bootstrap CSS -->
        <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css" integrity="sha384-9aIt2nRpC12Uk9gS9baDl411NQApFmC26EwAOH8WgZl5MYYxFfc+NcPb1dKGj7Sk" crossorigin="anonymous">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/ekko-lightbox/5.3.0/ekko-lightbox.css">
        <style>
        
        .grid-item {
            width: 240px;
            height: 240px;
            margin: 16px;
        }
        
        img {
            max-width: 100%;
            max-height: 100%;
        }
        
        </style>
        </head>
        <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js" integrity="sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj" crossorigin="anonymous"></script>
        <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js" integrity="sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo" crossorigin="anonymous"></script>
        <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.min.js" integrity="sha384-OgVRvuATP1z7JjHLkuOU7Xw704+h835Lr+6QL9UvYjZE3Ipu6Tp75j7Bh/kR0JKI" crossorigin="anonymous"></script>
        <script src="https://unpkg.com/isotope-layout@3/dist/isotope.pkgd.min.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/ekko-lightbox/5.3.0/ekko-lightbox.min.js"></script>
        
        <body>
        <p><input id="showVideos" type="checkbox"> Show videos only</p>
        <p><input id="reverseOrder" type="checkbox"> Reverse order</p>
        <div class="grid">
        
    """.trimIndent()

    private val footer = """
        </div>
        </body>
        
        <script>
        var grid = $('.grid').isotope({
            itemSelector: '.grid-item',
            layoutMode: 'fitRows',
            getSortData: {
                number: '[data-number] parseInt'
            }
        });
        
        $('#showVideos').click(function() {
            if ($(this).prop("checked")) {
                grid.isotope({filter: '.video' });
            } else {
                grid.isotope({filter: '*' });
            }
        });
        
        $('#reverseOrder').click(function() {
            if ($(this).prop("checked")) {
                grid.isotope({
                    sortBy: 'number',
                    sortAscending: false
                });
            } else {
                grid.isotope({
                    sortBy: 'original-order',
                    sortAscending: true
                });
            }
        });
        
        $(document).on('click', '[data-toggle="lightbox"]', function(event) {
                event.preventDefault();
                $(this).ekkoLightbox();
            });
        </script>
        </html>
    """.trimIndent()

    @JvmStatic
    fun main(args: Array<String>) {
        val session = if (args[0].startsWith('#'))
            Session(args[0].substring(1), Session.Type.HASHTAG)
        else
            Session(args[0], Session.Type.PERSON)

        var remoteCount = 0
        var count = 0

        val file = File("output.html")
        file.createNewFile()

        val fos = file.outputStream()
        val osw = OutputStreamWriter(fos)
        val writer = BufferedWriter(osw)

        writer.use {
            writer.write(header)

            try {
                InstagramExecutor().execute(session, object : InstagramExecutor.Callback {
                    override fun isStopped() = false

                    override fun onRemoteCountDetermined(count: Int) {
                        remoteCount = count
                    }

                    override fun onMediaProcessed(media: Media) {
                        writer.write("""<div class="grid-item ${if (media.isVideo) "video" else ""}" data-number="$count">""")
                        writer.write("""<a href="https://www.instagram.com/p/${media.shortcode}" data-toggle="lightbox">""")
                        writer.write("""<img src="${media.displayUrl}"/>""")
                        writer.write("</a></div>")
                        writer.newLine()
                        println("Processed ${++count} out of $remoteCount")
                    }

                    override fun onError(code: Int, msg: String) = System.err.println("Error $code: $msg")

                })
            } catch (e: IOException) {
                e.printStackTrace()
            }

            writer.write(footer)

            writer.flush()
        }
    }
}