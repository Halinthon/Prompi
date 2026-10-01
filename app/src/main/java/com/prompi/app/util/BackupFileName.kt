package com.prompi.app.util

import java.time.LocalDate

/** Nombre sugerido para el archivo de copia de seguridad, p. ej. prompi-backup-2026-10-01.json */
fun defaultBackupFileName(date: LocalDate = LocalDate.now()): String = "prompi-backup-$date.json"
