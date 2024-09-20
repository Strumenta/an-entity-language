package com.strumenta.kolorize

import com.strumenta.kolorize.Kolor.ESCAPE

private const val BG_JUMP = 10

object Kolor {
    internal const val ESCAPE = '\u001B'

    internal const val RESET = "$ESCAPE[0m"

    fun foreground(
        string: String,
        color: Color,
    ) = color(string, color.foreground)

    fun background(
        string: String,
        color: Color,
    ) = color(string, color.background)

    private fun color(
        string: String,
        ansiString: String,
    ) = "$ansiString$string$RESET"
}

/**
 * An enumeration of colors supported by most terminals. Can be applied to both foreground and background.
 */
enum class Color(baseCode: Int) {
    BLACK(30),
    RED(196),
    GREEN(24),
    YELLOW(220),
    BLUE(27),
    MAGENTA(35),
    CYAN(36),
    LIGHT_GRAY(37),
    DARK_GRAY(90),
    LIGHT_RED(91),
    LIGHT_GREEN(118),
    LIGHT_YELLOW(93),
    LIGHT_BLUE(94),
    LIGHT_MAGENTA(95),
    LIGHT_CYAN(96),
    WHITE(255),
    ;

    /**
     * ANSI modifier string to apply the color to the text itself
     * */
    val foreground: String = "$ESCAPE[38;5;${baseCode}m"

    /**
     * ANSI modifier string to apply the color the text's background
     * */
    val background: String = "$ESCAPE[${baseCode + BG_JUMP}m"
}

fun String.black() = Kolor.foreground(this, Color.BLACK)

fun String.red() = Kolor.foreground(this, Color.RED)

fun String.green() = Kolor.foreground(this, Color.GREEN)

fun String.yellow() = Kolor.foreground(this, Color.YELLOW)

fun String.blue() = Kolor.foreground(this, Color.BLUE)

fun String.magenta() = Kolor.foreground(this, Color.MAGENTA)

fun String.cyan() = Kolor.foreground(this, Color.CYAN)

fun String.lightGray() = Kolor.foreground(this, Color.LIGHT_GRAY)

fun String.lightRed() = Kolor.foreground(this, Color.LIGHT_RED)

fun String.lightGreen() = Kolor.foreground(this, Color.LIGHT_GREEN)

fun String.lightYellow() = Kolor.foreground(this, Color.LIGHT_YELLOW)

fun String.lightBlue() = Kolor.foreground(this, Color.LIGHT_BLUE)

fun String.lightMagenta() = Kolor.foreground(this, Color.LIGHT_MAGENTA)

fun String.lightCyan() = Kolor.foreground(this, Color.LIGHT_CYAN)

fun String.lightWhite() = Kolor.foreground(this, Color.WHITE)

fun String.blackBackground() = Kolor.background(this, Color.BLACK)

fun String.redBackground() = Kolor.background(this, Color.RED)

fun String.greenBackground() = Kolor.background(this, Color.GREEN)

fun String.yellowBackground() = Kolor.background(this, Color.YELLOW)

fun String.blueBackground() = Kolor.background(this, Color.BLUE)

fun String.magentaBackground() = Kolor.background(this, Color.MAGENTA)

fun String.cyanBackground() = Kolor.background(this, Color.CYAN)

fun String.lightGrayBackground() = Kolor.background(this, Color.LIGHT_GRAY)

fun String.lightRedBackground() = Kolor.background(this, Color.LIGHT_RED)

fun String.lightGreenBackground() = Kolor.background(this, Color.LIGHT_GREEN)

fun String.lightYellowBackground() = Kolor.background(this, Color.LIGHT_YELLOW)

fun String.lightBlueBackground() = Kolor.background(this, Color.LIGHT_BLUE)

fun String.lightMagentaBackground() = Kolor.background(this, Color.LIGHT_MAGENTA)

fun String.lightCyanBackground() = Kolor.background(this, Color.LIGHT_CYAN)

fun String.lightWhiteBackground() = Kolor.background(this, Color.WHITE)
