# Guía: subir Prompi a GitHub y obtener el APK desde Windows

Esta guía no requiere experiencia previa. Hay dos caminos para subir el código: **GitHub Desktop** (recomendado, sin comandos) o **Git en la terminal**. Después, GitHub compila el APK por ti, sin necesidad de instalar Android Studio.

---

## 1. Preparativos (una sola vez)

1. Crea una cuenta gratuita en <https://github.com/signup> si aún no la tienes.
2. Descarga e instala **GitHub Desktop** desde <https://desktop.github.com>. Al abrirlo, pulsa **Sign in to GitHub.com** e inicia sesión en el navegador.
3. Configura tu nombre y correo cuando te lo pida (aparecerán en el historial de cambios).

## 2. Descomprimir el proyecto

1. Crea una carpeta de trabajo corta, por ejemplo `C:\Proyectos`.
   - Evita **OneDrive**, el Escritorio sincronizado o rutas muy largas: OneDrive bloquea archivos durante la compilación y Windows tiene límites de longitud de ruta.
2. Clic derecho sobre `Prompi.zip` › **Extraer todo…** › elige `C:\Proyectos` › **Extraer**.
3. Comprueba que existe `C:\Proyectos\Prompi\settings.gradle.kts`. Si ves `C:\Proyectos\Prompi\Prompi\…`, mueve la carpeta interior un nivel arriba.
4. Activa los archivos ocultos para verificar que se extrajo todo: en el Explorador, **Ver › Mostrar › Elementos ocultos**. Deben aparecer la carpeta `.github` y los archivos `.gitignore` y `.gitattributes`.

## 3A. Subir a GitHub con GitHub Desktop (recomendado)

1. En GitHub Desktop: **File › Add local repository…** › **Choose…** › selecciona `C:\Proyectos\Prompi` › **Add repository**.
2. Aparecerá el aviso *This directory does not appear to be a Git repository*. Pulsa el enlace **create a repository**.
3. En la ventana que se abre:
   - **Name:** `Prompi`
   - **Git ignore:** `None` y **License:** `None` (el proyecto ya incluye ambos).
   - Pulsa **Create repository**.
4. GitHub Desktop crea automáticamente el primer *commit* con todos los archivos. Comprueba en la pestaña **History** que aparece el commit *Initial commit*.
5. Pulsa **Publish repository** (barra superior).
   - Marca **Keep this code private** si no quieres que sea público. Ambas opciones funcionan con la compilación automática.
   - Pulsa **Publish repository**.
6. Pulsa **View on GitHub** para abrir tu repositorio en el navegador.

## 3B. Alternativa: subir con Git en la terminal

1. Instala **Git for Windows** desde <https://git-scm.com/download/win> (deja las opciones por defecto).
2. En <https://github.com/new> crea un repositorio llamado `Prompi` **vacío**: no marques README, .gitignore ni licencia. Pulsa **Create repository**.
3. Abre **PowerShell** en la carpeta del proyecto (en el Explorador, clic derecho dentro de `C:\Proyectos\Prompi` › **Abrir en Terminal**) y ejecuta, sustituyendo `TU_USUARIO`:

```powershell
git config --global core.longpaths true
git init -b main
git add .
git update-index --chmod=+x gradlew
git commit -m "Primera versión de Prompi"
git remote add origin https://github.com/TU_USUARIO/Prompi.git
git push -u origin main
```

La primera vez que hagas `push` se abrirá el navegador para iniciar sesión en GitHub. La línea `git update-index --chmod=+x gradlew` marca el script de Gradle como ejecutable, algo que Windows no puede hacer por sí solo.

## 4. Obtener el APK compilado por GitHub

1. En tu repositorio, abre la pestaña **Actions**.
   - Si aparece un aviso para habilitar los *workflows*, pulsa **I understand my workflows, go ahead and enable them**.
2. Verás una ejecución llamada **Compilar APK** (se inicia sola con cada subida). Si no aparece, elige **Compilar APK** a la izquierda › **Run workflow** › **Run workflow**.
3. Espera a que el círculo amarillo cambie a una marca verde ✓ (la primera vez tarda unos 5–10 minutos).
4. Abre esa ejecución y baja hasta **Artifacts**. Descarga **prompi-debug-apk**: es un ZIP que contiene `app-debug.apk`.
   - Los artefactos se conservan 90 días; puedes volver a generarlos cuando quieras con **Run workflow**.
5. Si aparece una ✗ roja, abre la ejecución, pulsa el paso que falló y copia el texto del error para revisarlo.

## 5. Instalar el APK en el móvil (Android 12 o superior)

1. Descomprime `prompi-debug-apk.zip` en el PC y copia `app-debug.apk` al móvil (cable USB, Google Drive, correo…).
2. En el móvil, abre el archivo desde **Archivos** o **Descargas**.
3. Android pedirá permitir **Instalar aplicaciones desconocidas** para esa app: actívalo y vuelve atrás.
4. Pulsa **Instalar**. Si Google Play Protect muestra un aviso (es normal en apps que no vienen de la Play Store), elige **Más detalles › Instalar de todos modos**.

La versión de depuración se llama **Prompi** y su identificador es `com.prompi.app.debug`, así que podrá convivir con una futura versión Release.

## 6. Opcional: abrir y compilar con Android Studio en Windows

1. Instala **Android Studio** (2025.1 o posterior) desde <https://developer.android.com/studio> con las opciones por defecto.
2. **File › Open** › selecciona `C:\Proyectos\Prompi` › **Trust Project**.
3. Espera a que termine la sincronización de Gradle (barra inferior). Si pide instalar el **SDK Platform 36**, acepta.
4. Para probar en tu móvil: activa **Opciones de desarrollador › Depuración por USB**, conéctalo y pulsa ▶ **Run**. También puedes crear un emulador en **Device Manager**.
5. Desde la terminal de Android Studio (pestaña **Terminal**):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

El APK queda en `app\build\outputs\apk\debug\app-debug.apk`.

## 7. Opcional: APK Release firmado en Windows

1. Crea la clave con la herramienta incluida en Android Studio (todo en una línea):

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore prompi-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias prompi
```

2. Guarda `prompi-release.jks` y sus contraseñas en un lugar seguro, fuera de OneDrive público. **Si los pierdes, no podrás actualizar la app instalada.**
3. Copia `keystore.properties.example` como `keystore.properties` y rellénalo. Usa barras normales en las rutas: `storeFile=C:/Claves/prompi-release.jks`.
4. Ejecuta `.\gradlew.bat assembleRelease`. El APK queda en `app\build\outputs\apk\release\app-release.apk`.

`keystore.properties` y los archivos `.jks` están en `.gitignore`: nunca se subirán a GitHub.

## 8. Subir cambios más adelante

- **GitHub Desktop:** los archivos modificados aparecen en **Changes**. Escribe un resumen abajo a la izquierda, pulsa **Commit to main** y luego **Push origin**. GitHub compilará un APK nuevo.
- **Terminal:** `git add .`, `git commit -m "Descripción del cambio"` y `git push`.
- Tras la primera compilación local se crea la carpeta `app\schemas`: súbela en el siguiente commit (es necesaria para futuras migraciones de la base de datos).

## 9. Problemas frecuentes

| Problema | Solución |
|---|---|
| En Actions: `Permission denied` al ejecutar `gradlew` | El *workflow* ya ejecuta `chmod +x`; si persiste, usa el comando `git update-index --chmod=+x gradlew` del paso 3B y vuelve a subir. |
| `/usr/bin/env: 'sh\r': No such file or directory` | `gradlew` se subió con saltos de línea de Windows. El archivo `.gitattributes` lo evita; asegúrate de que se subió y vuelve a guardar `gradlew`. |
| No aparece la pestaña o la ejecución en **Actions** | Comprueba que la carpeta `.github\workflows` se subió (activa los elementos ocultos y revisa el repositorio en la web). |
| `SDK location not found` al compilar en local | Abre el proyecto una vez en Android Studio: crea `local.properties` con la ruta del SDK. |
| `Unsupported class file major version` o error de JDK | Usa el JDK de Android Studio (**Settings › Build Tools › Gradle › Gradle JDK › jbr-17 o superior**). |
| `Filename too long` | Ejecuta `git config --global core.longpaths true` y usa una ruta corta como `C:\Proyectos`. |
| La compilación local falla con archivos bloqueados | Saca el proyecto de OneDrive o pausa la sincronización. |
| El móvil dice «La app no se ha instalado» | Desinstala una versión anterior firmada con otra clave, o comprueba que el móvil tiene Android 12 o superior. |
