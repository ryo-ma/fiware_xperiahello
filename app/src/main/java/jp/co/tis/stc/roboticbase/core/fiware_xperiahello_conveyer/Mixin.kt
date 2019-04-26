package jp.co.tis.stc.roboticbase.core.fiware_xperiahello_conveyer

interface Mixin {
    val TAG: String
        get() = this.javaClass.name
    val OPERATION_BACK_CODE
        get() = 1001
    val PERMISSION_REQUEST_CODE
        get() = 100
    val OPERATION_RESULT_KEY
        get() = "RESULT_KEY"
    val CERTIFICATE_NAME
        get() = "tis.pem"
    val TRIANGLE_BUTTON
        get() = "triangle"
    val SQUARE_BUTTON
        get() = "square"
    val CIRCLE_BUTTON
        get() = "circle"
    val DO_FORWARD_BUTTON
        get() = "do_forward"
    val CROSS_BUTTON
        get() = "cross"
}