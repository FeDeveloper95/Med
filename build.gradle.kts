// Rimuovi tutto ciò che c'è prima di questo blocco, specialmente se c'è scritto "buildscript"
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
// ... resto del file