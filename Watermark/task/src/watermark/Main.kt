package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun getFile(name: String, type: String): BufferedImage {
    try {
        val image = ImageIO.read(File(name))
        if (image.colorModel.numComponents != 3 && image.colorModel.numComponents != 4) println("The number of $type color components isn't 3."). also { exitProcess(-2) }

        if (image.colorModel.pixelSize != 24 && image.colorModel.pixelSize != 32) println("The $type isn't 24 or 32-bit.").also { exitProcess(-3) }

        return image
    } catch (exc: java.io.IOException) {
        println("The file $name doesn't exist.").also { exitProcess(-1) }
    }
}

fun main() {
    println("Input the image filename:")
    val image = getFile(readln(), "image")
    println("Input the watermark image filename:")
    val watermark = getFile(readln(), "watermark")

    if (image.width < watermark.width || image.height < watermark.height)
        println("The watermark's dimensions are larger.").also { exitProcess(-4) }

    var useColorList = mutableListOf<Int>()

    var useAlpha = false
    if (watermark.transparency == 3) {
        println("Do you want to use the watermark's Alpha channel?")
        if (readln() == "yes") {
            useAlpha = true
        }
    } else {

        println("Do you want to set a transparency color?")
        if (readln() == "yes") {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            val userColor = readln()

            if (!Regex("^([0-2]?[0-5]?[0-5] ?){3}\$").matches(userColor)) {
                println("The transparency color input is invalid.")
                exitProcess(-8)
            } else {
                useColorList  = userColor.split(" ").map { it.toInt() }.toMutableList()
            }
        }
    }

    println("Input the watermark transparency percentage (Integer 0-100):")
    val weight = try {
        readln().toInt()
    } catch (exc: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(-5)
    }.also {
        if (it !in 0..100) println("The transparency percentage is out of range.").also { exitProcess(-6) }
    }

    var logoPositionList = mutableListOf<Int>()
    println("Choose the position method (single, grid):")
    when (readln()) {
        "single" -> {
            println("Input the watermark position ([x 0-${image.width - watermark.width}] [y 0-${image.height - watermark.height}]):")
            val logoPosition = readln()
            if (!Regex("^([-0-9]?[-0-9]?[-0-9] ?){2}\$").matches(logoPosition)) {
                println("The position input is invalid.")
                exitProcess(-8)
            } else {
                logoPositionList  = logoPosition.split(" ").map { it.toInt() }
                    .filterIndexed { i, s ->
                        ( i == 0 && s >= 0 && s <= image.width - watermark.width ) ||
                                (i == 1 && s >= 0 && s <= image.height - watermark.height) }.toMutableList()
                if (logoPositionList.size < 2) {
                    println("The position input is out of range.")
                    exitProcess(-8)
                }
            }
        }
        "grid" -> Unit
        else -> println("The position method input is invalid.").also { exitProcess(-9) }
    }

    println("Input the output image filename (jpg or png extension):")
    val outputImagePath = readln()
    val fileExtension = outputImagePath.reversed().subSequence(0, 3).reversed().toString()
    if (fileExtension != "jpg" && fileExtension != "png") {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(-7)
    }

    val outputImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)

    for (row in 0 until image.height) {
        for (col in 0 until image.width) {

            val i = Color(image.getRGB(col, row))

            val w = if (logoPositionList.isEmpty()) {
                //grid
                Color(watermark.getRGB(col % watermark.width, row % watermark.height), true)
            } else {
                //single
                if (col >= logoPositionList[0] &&
                    row >= logoPositionList[1] &&
                    col < logoPositionList[0] + watermark.width &&
                    row < logoPositionList[1] + watermark.height ) {

                    Color(watermark.getRGB(col - logoPositionList[0], row - logoPositionList[1]), true)
                } else {
                    Color(image.getRGB(col, row))
                }
            }

            val color = if (useAlpha && w.alpha == 0) {
                Color(image.getRGB(col, row))
            } else if (useColorList.isNotEmpty() && w.rgb == Color(useColorList.component1(), useColorList.component2(), useColorList.component3()).rgb) {
                Color(image.getRGB(col, row))
            } else {
                Color(
                    (weight * w.red + (100 - weight) * i.red) / 100,
                    (weight * w.green + (100 - weight) * i.green) / 100,
                    (weight * w.blue + (100 - weight) * i.blue) / 100
                )
            }
            outputImage.setRGB(col, row, color.rgb)
        }
    }

    ImageIO.write(outputImage, fileExtension, File(outputImagePath))
    println("The watermarked image $outputImagePath has been created.")
}
