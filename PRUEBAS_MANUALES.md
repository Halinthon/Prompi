# Pruebas manuales de Prompi

Lista para comprobar en un dispositivo o emulador (Android 12 o superior) antes de publicar una versión. Marca cada punto al verificarlo.

## Compilación
- [ ] `./gradlew assembleDebug testDebugUnitTest` termina sin errores.
- [ ] `./gradlew connectedDebugAndroidTest` pasa con un emulador abierto.
- [ ] `./gradlew assembleRelease` genera el APK y la versión Release abre y funciona igual (colores, tema, importar/exportar).

## Fichas y categorías
- [ ] Crear, renombrar y eliminar categorías; no se permite un nombre vacío ni repetido (aunque cambien mayúsculas).
- [ ] Eliminar una categoría con fichas: mover a otra (la primera viene marcada) o eliminar todas.
- [ ] La ficha nueva aparece arriba, con 1 estrella y color Gris.
- [ ] El título no admite más de 50 caracteres ni saltos de línea; el contenido, no más de 7000. Pegar un texto largo lo recorta.
- [ ] Guardar sin título muestra el error; salir con cambios pide confirmación.
- [ ] Tocar una ficha la expande y la contrae; mantenerla pulsada abre el selector de color.
- [ ] Copiar muestra «Contenido copiado» y pega solo el contenido; compartir envía título y contenido.
- [ ] Eliminar pide confirmación y «Deshacer» la recupera en su sitio, con su color y favorito.
- [ ] Los contadores de cada categoría y el total se actualizan al momento.

## Reordenar
- [ ] Arrastrar desde el asa reordena categorías y fichas; el orden se mantiene al cerrar y abrir la app.
- [ ] «Mover arriba/abajo» del menú funciona y no aparece en el primer/último elemento.
- [ ] Tocar la ficha cerca del asa no inicia un arrastre accidental.

## Búsqueda y favoritos
- [ ] «cafe» encuentra «Café»; «CORR» encuentra «correo»; varias palabras exigen todas.
- [ ] Los resultados muestran la categoría y los que coinciden en el título salen primero.
- [ ] Caracteres raros (`" * ( ) -`) no producen errores.
- [ ] Marcar y desmarcar favoritos se refleja en la pestaña Favoritos.

## Exportar e importar
- [ ] Exportar crea `prompi-backup-AAAA-MM-DD.json` y el mensaje indica el número de fichas.
- [ ] Importar ese archivo en «Combinar» no duplica fichas existentes.
- [ ] «Reemplazar todo» deja exactamente los datos del archivo.
- [ ] Un archivo que no es de Prompi, un JSON dañado o una imagen muestran un mensaje claro y no cambian nada.
- [ ] Cancelar el selector de archivos no muestra errores.

## Rotación, temas y sistema
- [ ] Girar la pantalla con el editor, un diálogo, el selector de color o la búsqueda abiertos conserva todo.
- [ ] En horizontal nada queda bajo la cámara, la barra de estado ni la de navegación.
- [ ] El teclado no tapa el campo que se está escribiendo en el editor.
- [ ] Sistema, Claro y Oscuro se aplican al momento, se recuerdan y no hay destello al abrir la app.
- [ ] Opciones de desarrollador › «No mantener actividades»: salir y volver al editor conserva el borrador.

## Accesibilidad
- [ ] Con TalkBack: cada botón se anuncia («Copiar contenido», «Calificar con 3 estrellas»…) y las acciones de la ficha están en el menú de acciones (copiar, favorito, mover).
- [ ] Con fuente al 200 % los textos no se cortan de forma que impida usar la app.
- [ ] Con «Escáner de accesibilidad» no hay avisos de áreas táctiles pequeñas ni de contraste.

## Rendimiento
- [ ] Con 500 fichas (importa un JSON grande) el desplazamiento es fluido y la búsqueda responde al instante.
- [ ] Expandir una ficha de 7000 caracteres no provoca saltos.
