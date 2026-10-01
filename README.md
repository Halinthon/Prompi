# Prompi

Prompi es una aplicación Android para guardar, organizar y reutilizar tus *prompts* (fichas de texto) por categorías. Funciona **100 % sin conexión**: no pide permisos, no usa Internet y tus datos nunca salen del dispositivo, salvo cuando tú exportas o compartes una ficha.

## Funciones

Prompi organiza las fichas en categorías, cada una con su contador de fichas, y muestra el total en la pantalla principal. Cada ficha tiene un título obligatorio (hasta 50 caracteres), un contenido de hasta 7000 caracteres, una calificación de 1 a 5 estrellas (1 por defecto), un color y la marca de favorito. Las fichas se ven contraídas en unas cuatro líneas y se expanden al tocarlas; el botón de la derecha copia solo el contenido al portapapeles, y al mantener pulsada una ficha aparece el selector de cinco colores suaves (Gris, Azul claro, Rosa, Amarillo y Verde), adaptados al tema claro y oscuro.

También incluye búsqueda en títulos y contenido (sin distinguir mayúsculas ni tildes, y por prefijo), una pantalla de Favoritos, reordenación de categorías y fichas arrastrando el asa (con alternativas «Mover arriba/abajo» accesibles), confirmación antes de eliminar y opción de deshacer, compartir título y contenido con otras apps, tema Sistema, Claro u Oscuro que se recuerda, y exportación e importación en JSON (combinando con los datos actuales o reemplazándolos).

## Arquitectura

| Capa | Contenido |
|---|---|
| `ui/` | Pantallas en Jetpack Compose + Material 3, ViewModels (MVVM) y navegación tipada |
| `domain/` | Modelos, interfaces de repositorio y construcción de consultas de búsqueda |
| `data/` | Room (SQLite + FTS4), DataStore (tema) y copias de seguridad con kotlinx.serialization |
| `di/` | `AppContainer`: inyección de dependencias manual, sin librerías extra |

La búsqueda usa una tabla FTS4 con el tokenizador `unicode61 remove_diacritics=2`, que encuentra «cafe» dentro de «Café». La copia de seguridad de Android está desactivada (`allowBackup=false` y `data_extraction_rules.xml`, que también bloquea la transferencia entre dispositivos), así que la única forma de trasladar datos es la exportación JSON.

## Inicio rápido desde Windows

Consulta **[GUIA_GITHUB_WINDOWS.md](GUIA_GITHUB_WINDOWS.md)**: explica paso a paso cómo subir el proyecto a GitHub con GitHub Desktop o con Git, descargar el APK que compila GitHub Actions e instalarlo en el móvil, sin necesidad de Android Studio.

## Requisitos

- Android Studio Narwhal (2025.1) o posterior, con el SDK de Android 36 instalado.
- JDK 17 (Android Studio ya incluye uno).
- Dispositivo o emulador con **Android 12 (API 31)** o superior.

## Abrir y compilar

```bash
git clone https://github.com/TU_USUARIO/Prompi.git
cd Prompi
```

En Android Studio: **File › Open**, selecciona la carpeta `Prompi` y espera a que termine la sincronización de Gradle. Pulsa ▶ **Run** para instalarla en tu dispositivo.

Desde la terminal (en Windows usa `.\gradlew.bat` en lugar de `./gradlew`):

```bash
./gradlew assembleDebug          # APK de depuración
./gradlew testDebugUnitTest      # pruebas unitarias
./gradlew connectedDebugAndroidTest   # pruebas de base de datos (requiere dispositivo o emulador)
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. La versión de depuración se instala como `com.prompi.app.debug`, por lo que puede convivir con la versión Release.

> **Tras la primera compilación**, Room genera `app/schemas/com.prompi.app.data.local.PrompiDatabase/1.json`. Súbelo a Git: es necesario para escribir y probar migraciones cuando cambie la base de datos.

## APK Release firmado

1. Crea un almacén de claves (una sola vez) y guárdalo en un lugar seguro. Si lo pierdes, no podrás publicar actualizaciones:
   ```bash
   keytool -genkeypair -v -keystore prompi-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias prompi
   ```
2. Copia `keystore.properties.example` como `keystore.properties` en la raíz y rellena los datos. Ese archivo y los `.jks` están en `.gitignore`.
3. Compila:
   ```bash
   ./gradlew assembleRelease
   ```
   El resultado queda en `app/build/outputs/apk/release/app-release.apk` (minificado con R8). Sin `keystore.properties`, Gradle genera un APK *unsigned* que no se puede instalar.

También puedes usar **Build › Generate Signed App Bundle / APK** en Android Studio.

## Compilar en GitHub (sin Android Studio)

El flujo `.github/workflows/build.yml` compila el APK de depuración y ejecuta las pruebas unitarias en cada *push* a `main`, en cada *pull request* o a mano desde la pestaña **Actions › Compilar APK › Run workflow**. Al terminar, descarga el APK desde la sección **Artifacts** del resultado.

## Versiones principales

| Componente | Versión |
|---|---|
| Android Gradle Plugin | 8.10.1 |
| Gradle | 8.14.3 |
| Kotlin / KSP | 2.1.21 / 2.1.21-2.0.1 |
| Compose BOM | 2025.06.00 |
| Navigation Compose | 2.9.0 |
| Lifecycle | 2.9.1 |
| Room | 2.7.1 |
| DataStore | 1.1.7 |
| kotlinx.serialization | 1.8.1 |
| Reorderable (Calvin-LL) | 2.4.3 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 31 |

Todas las versiones están en `gradle/libs.versions.toml`.

## Formato de copia de seguridad

```json
{
  "app": "Prompi",
  "formatVersion": 1,
  "exportedAt": "2026-10-01T10:00:00Z",
  "categories": [
    {
      "name": "Correos",
      "position": 0,
      "createdAt": 1727776800000,
      "updatedAt": 1727776800000,
      "cards": [
        {
          "title": "Respuesta formal",
          "content": "Estimado/a…",
          "rating": 4,
          "isFavorite": true,
          "color": "BLUE",
          "position": 0,
          "createdAt": 1727776800000,
          "updatedAt": 1727776800000
        }
      ]
    }
  ]
}
```

Los colores válidos son `GRAY`, `BLUE`, `PINK`, `YELLOW` y `GREEN`. Al importar, los valores fuera de rango (títulos largos, estrellas inválidas, colores desconocidos) se corrigen en lugar de rechazar el archivo, y toda la importación se hace en una sola transacción: si algo falla, tus datos no cambian.

## Mantenimiento

- **Actualizar dependencias:** cambia la versión en `gradle/libs.versions.toml`, sincroniza y ejecuta las pruebas. Kotlin, KSP y el plugin de Compose deben ir siempre emparejados (la versión de KSP empieza por la de Kotlin).
- **Cambiar la base de datos:** incrementa `version` en `PrompiDatabase`, añade una `Migration` explícita y sube el nuevo esquema de `app/schemas`. No se usan migraciones destructivas para no perder datos.
- **Cambiar el formato JSON:** incrementa `CURRENT_FORMAT_VERSION` en `BackupCodec` y mantén la lectura de las versiones anteriores.
- **Textos:** todos están en `app/src/main/res/values/strings.xml`. Para traducir la app, crea `values-en/strings.xml`, etc.

### Nota sobre permisos

Prompi no declara permisos. Al revisar el manifiesto final puede aparecer `com.prompi.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`: lo añade automáticamente la librería `androidx.core`, es interno de la propia app, no se muestra al usuario y no da acceso a nada.

## Estructura del proyecto

```
Prompi/
├── .github/workflows/build.yml   Compilación automática en GitHub
├── app/
│   ├── build.gradle.kts          Configuración del módulo (SDK, firma, dependencias)
│   ├── proguard-rules.pro        Reglas de R8 para Release
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/prompi/app/
│       │   │   ├── data/         Room, DataStore, copias de seguridad y repositorios
│       │   │   ├── di/           AppContainer
│       │   │   ├── domain/       Modelos, interfaces y búsqueda
│       │   │   ├── ui/           Pantallas, componentes, navegación y tema
│       │   │   └── util/         Portapapeles, compartir y nombres de archivo
│       │   └── res/              Textos, temas, colores e ícono
│       ├── test/                 Pruebas unitarias
│       └── androidTest/          Pruebas de base de datos
├── gradle/libs.versions.toml     Versiones de todas las dependencias
├── GUIA_GITHUB_WINDOWS.md        Guía paso a paso para Windows
├── PRUEBAS_MANUALES.md           Lista de comprobación antes de publicar
└── README.md
```

## Licencia

MIT. Consulta el archivo [LICENSE](LICENSE).
