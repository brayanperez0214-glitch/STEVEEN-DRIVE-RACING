# STEVEEN-DRIVE — Chevrolet Aveo

Proyecto Android nativo en Java, preparado para Android 11 y pantalla horizontal 1024×600.

## Qué incluye
- Usa la imagen aprobada `dashboard.png` como fondo íntegro de la pantalla principal.
- Modo inmersivo horizontal.
- Velocidad GPS en km/h superpuesta sobre el número central del velocímetro.
- Zonas táctiles para Mapas, Bluetooth, WhatsApp, Música, Video, Auto, Aplicaciones, Configuración y Teléfono.
- No modifica firmware ni requiere root.

## Compilar en GitHub
1. Sube todo el contenido de este ZIP a la raíz de un repositorio GitHub.
2. Abre **Actions**.
3. Ejecuta **Build STEVEEN-DRIVE APK**.
4. Descarga el artefacto `STEVEEN-DRIVE-debug-apk`.
5. Dentro encontrarás `app-debug.apk`.

## Requisitos de compilación
- JDK 17
- Gradle 8.7
- Android Gradle Plugin 8.6.1
- Android SDK Platform 35

## Instalar y probar
Instala el APK en el radio. Al abrir por primera vez, concede el permiso de ubicación precisa y activa GPS.
El velocímetro usa la velocidad GPS reportada por Android; requiere señal GPS y puede tardar en fijar satélites.

## Importante sobre OBD2
Esta versión no lee todavía el ELM327. RPM, temperatura, voltaje y combustible permanecen dibujados en la imagen de referencia y no deben interpretarse como lecturas en vivo. La integración OBD2 requiere emparejar y probar el adaptador y confirmar su protocolo/permisos Bluetooth.
