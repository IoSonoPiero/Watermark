package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

const val INPUT_FILE = "image"
const val WATERMARK = "watermark"
const val OUTPUT_FILE = "output"

fun main() {
    // get the input file and do checks
    val inputImage = getInputFile(INPUT_FILE)
    checkNumColorComponents(INPUT_FILE, inputImage)
    checkBitsPerPixel(INPUT_FILE, inputImage)

    // get the watermark file and do checks
    val watermarkImage = getInputFile("$WATERMARK $INPUT_FILE")
    checkNumColorComponents(WATERMARK, watermarkImage)
    checkBitsPerPixel(WATERMARK, watermarkImage)

    // compare the size of input and watermark files
    compareSize(WATERMARK, watermarkImage, inputImage)

    // check if watermark has transparency property set
    // eventually choose to disable alpha channel usage
    val hasAlpha = checkTransparency(watermarkImage)

    var useAlpha = false

    // if hasAlpha is true, then ask to use it or not
    if (hasAlpha) {
        useAlpha = askToUseAlpha()
    }

    var hasForcedAlpha = false
    var userAlphaColor = Color(0, 0, 0)

    if (!hasAlpha) {
        val (alpha, color) = askForColorTransparency()
        hasForcedAlpha = alpha
        userAlphaColor = color
    }

    // determine the wanted color weight percentage
    val colorWeightPercentage = colorWeightPercentage(WATERMARK)

    // ask positioning method and coordinates
    val (positionMethod, coordinates) = choosePositionMethod(inputImage, watermarkImage)

    // return a BufferedImage output and the final output file
    val (outputImage, outputFile) = getOutputImage(OUTPUT_FILE, inputImage.width, inputImage.height)

    if (positionMethod == "single") {
        putWatermarkSingle(
            inputImage,
            watermarkImage,
            outputImage,
            outputFile,
            colorWeightPercentage,
            useAlpha,
            hasForcedAlpha,
            userAlphaColor,
            coordinates
        )
    } else {
        putWatermarkGrid(
            inputImage,
            watermarkImage,
            outputImage,
            outputFile,
            colorWeightPercentage,
            useAlpha,
            hasForcedAlpha,
            userAlphaColor
        )
    }
}

// check if position method is correct
// returns position method and coordinates
fun choosePositionMethod(inputImage: BufferedImage, watermarkImage: BufferedImage): Pair<String, List<Int>> {
    println("Choose the position method (single, grid):")
    val method = readln()
    if (method !in listOf("single", "grid")) {
        println("The position method input is invalid.")
        exitProcess(1)
    }
    if (method == "single") {
        val diffX = inputImage.width - watermarkImage.width
        val diffY = inputImage.height - watermarkImage.height
        println("Input the watermark position ([x 0-$diffX] [y 0-$diffY]):")

        try {
            val coordinates = readln().split(" ").map { it.toInt() }
            // check if specified X and Y are in range
            if (checkIfAValuesInRange(
                    coordinates, 2, 0, 0, diffX
                ) && checkIfAValuesInRange(
                    coordinates, 2, 1, 0, diffY
                )
            ) {
                return "single" to coordinates
            } else {
                println("The position input is out of range.")
                exitProcess(1)
            }
        } catch (e: Exception) {
            println("The position input is invalid.")
            exitProcess(1)
        }

    } else {
        return "grid" to listOf(0, 0)
    }
}

fun askToUseAlpha(): Boolean {
    println("Do you want to use the watermark's Alpha channel?")
    val answer = readln().lowercase()
    return answer == "yes"
}

// ask for an alpha channel if alpha is needed
fun askForColorTransparency(): Pair<Boolean, Color> {
    println("Do you want to set a transparency color?")

    if (readln().lowercase() == "yes") {
        println("Input a transparency color ([Red] [Green] [Blue]):")
        try {
            val color = readln().split(" ").map { it.toInt() }
            if (checkIfAllValuesInRange(color, 3, 0, 255)) {
                return true to Color(color[0], color[1], color[2])
            } else {
                println("The transparency color input is invalid.")
                exitProcess(1)
            }
        } catch (e: Exception) {
            println("The transparency color input is invalid.")
            exitProcess(1)
        }
    } else {
        return false to Color(0, 0, 0)
    }
}

// check if all values are an exact size with between min and max
fun checkIfAllValuesInRange(aList: List<Int>, listSize: Int, lower: Int, upper: Int): Boolean {
    return aList.size == listSize && aList.any { it in lower..upper }
}

// check if a single value is an exact size with between min and max
fun checkIfAValuesInRange(aList: List<Int>, listSize: Int, positionOfValue: Int, lower: Int, upper: Int): Boolean {
    return (aList.size == listSize && aList[positionOfValue] in lower..upper)
}

fun checkTransparency(watermarkImage: BufferedImage): Boolean {
    return when (watermarkImage.transparency) {
        1 -> false  // "OPAQUE"
        2 -> false  // "BITMASK"
        3 -> true   // "TRANSLUCENT"
        else -> false // "UNKNOWN"
    }
}

fun putWatermarkSingle(
    inputImage: BufferedImage,
    watermarkImage: BufferedImage,
    outputImage: BufferedImage,
    outputFile: File,
    colorWeightPercentage: Int,
    useAlpha: Boolean,
    hasForcedAlpha: Boolean,
    userAlphaColor: Color,
    coordinates: List<Int>
) {
    // For each pixel position (x, y) specified in the input images,
    // read the Color from the watermarked image and the Color from the watermark image

    // set start coordinates to value chosen by user
    val startX = coordinates[0]
    val startY = coordinates[0]

    for (x in 0 until inputImage.width) {
        for (y in 0 until inputImage.height) {

            val i = Color(inputImage.getRGB(x, y))

            if ((x in startX until startX + watermarkImage.width) && (y in startY until startY + watermarkImage.height)) {

                // get alpha channel if present
                val w = Color(watermarkImage.getRGB(x - startX, y - startY), true)

                // determine the color to use as alpha
                val color =
                    if ((hasForcedAlpha && w == userAlphaColor) || (useAlpha && w.alpha == 0)) {
                        i
                    } else {
                        Color(
                            (colorWeightPercentage * w.red + (100 - colorWeightPercentage) * i.red) / 100,
                            (colorWeightPercentage * w.green + (100 - colorWeightPercentage) * i.green) / 100,
                            (colorWeightPercentage * w.blue + (100 - colorWeightPercentage) * i.blue) / 100
                        )
                    }
                // Set the color at the output BufferedImage instance at position (x, y)
                outputImage.setRGB(x, y, color.rgb)
            } else {

                // Set the color at the output BufferedImage instance at position (x, y)
                outputImage.setRGB(x, y, i.rgb)
            }
        }
    }

    // save image to disk
    ImageIO.write(outputImage, outputFile.extension, outputFile)
    println("The watermarked image ${outputFile.path} has been created.")
}

fun putWatermarkGrid(
    inputImage: BufferedImage,
    watermarkImage: BufferedImage,
    outputImage: BufferedImage,
    outputFile: File,
    colorWeightPercentage: Int,
    useAlpha: Boolean,
    hasForcedAlpha: Boolean,
    userAlphaColor: Color
) {
    // For each pixel position (x, y) specified in the input images,
    // read the Color from the watermarked image and the Color from the watermark image

    // set start coordinates to value chosen by user
    for (x in 0 until inputImage.width) {
        for (y in 0 until inputImage.height) {

            val i = Color(inputImage.getRGB(x, y))

            // get alpha channel if present
            val w = Color(watermarkImage.getRGB(x % watermarkImage.width, y % watermarkImage.height), true)

            // determine the color to use as alpha
            val color =
                if ((hasForcedAlpha && w == userAlphaColor) || (useAlpha && w.alpha == 0)) {
                    i
                } else {
                    Color(
                        (colorWeightPercentage * w.red + (100 - colorWeightPercentage) * i.red) / 100,
                        (colorWeightPercentage * w.green + (100 - colorWeightPercentage) * i.green) / 100,
                        (colorWeightPercentage * w.blue + (100 - colorWeightPercentage) * i.blue) / 100
                    )
                }
            // Set the color at the output BufferedImage instance at position (x, y)
            outputImage.setRGB(x, y, color.rgb)
        }
    }

// save image to disk
    ImageIO.write(outputImage, outputFile.extension, outputFile)
    println("The watermarked image ${outputFile.path} has been created.")
}

// return an ImageIO instance from a filename
fun getInputFile(fileType: String): BufferedImage {
    println("Input the $fileType filename:")
    val filename = File(readln())
    if (!filename.exists()) {
        println("The file ${filename.path} doesn't exist.")
        exitProcess(1)
    }

    val myImage: BufferedImage
    try {
        myImage = ImageIO.read(filename)
    } catch (e: Exception) {
        println("The file $filename doesn't exist.")
        exitProcess(1)
    }

    return myImage
}

// return an ImageIO instance from a filename
fun getOutputImage(fileType: String, width: Int, height: Int): Pair<BufferedImage, File> {
    println("Input the $fileType image filename (jpg or png extension):")
    val outputFile = File(readln())
    if (outputFile.extension.lowercase() !in listOf("jpg", "png")) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(1)
    }
    return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB) to outputFile
}

// check if image does have 3 color components
fun checkNumColorComponents(fileType: String, imageFile: BufferedImage) {
    if (imageFile.colorModel.numColorComponents != 3) {
        println("The number of $fileType color components isn't 3.")
        exitProcess(1)
    }
}

// check if image does have 24 or 32 bits per pixel
fun checkBitsPerPixel(fileType: String, imageFile: BufferedImage) {
    if (imageFile.colorModel.pixelSize !in listOf(24, 32)) {
        println("The $fileType isn't 24 or 32-bit.")
        exitProcess(1)
    }

}

// check if two images has same dimensions in pixel
fun compareSize(fileType1: String, imageFile1: BufferedImage, imageFile2: BufferedImage) {
    if (imageFile1.width > imageFile2.width || imageFile1.height > imageFile2.height) {
        println("The $fileType1's dimensions are larger.")
        exitProcess(1)
    }
}

// return the wanted color weight percentage
fun colorWeightPercentage(fileType: String): Int {
    println("Input the $fileType transparency percentage (Integer 0-100):")
    val percentage: Int
    try {
        percentage = readln().toInt()
    } catch (e: Exception) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(1)
    }
    if (percentage !in (0..100)) {
        println("The transparency percentage is out of range.")
        exitProcess(1)
    }
    return percentage
}

fun getImageInfo(inputName: String) {
    val filename = File(inputName)
    val myImage: BufferedImage

    try {
        myImage = ImageIO.read(filename)
    } catch (e: Exception) {
        println("The file $filename doesn't exist.")
        return
    }
    println("Image file: $inputName")
    println("Width: ${myImage.width}")
    println("Height: ${myImage.height}")
    println("Number of components: ${myImage.colorModel.numComponents}")
    println("Number of color components: ${myImage.colorModel.numColorComponents}")
    println("Bits per pixel: ${myImage.colorModel.pixelSize}")
    val transparency = when (myImage.transparency) {
        1 -> "OPAQUE"
        2 -> "BITMASK"
        3 -> "TRANSLUCENT"
        else -> "UNKNOWN"
    }
    println("Transparency: $transparency")
}