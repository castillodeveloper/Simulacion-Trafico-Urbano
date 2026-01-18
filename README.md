# Simulación de Tráfico Urbano

Proyecto desarrollado para el módulo **Programación de Servicios y Procesos (PSP)** del ciclo DAM, consistente en una aplicación Android que implementa un **simulador de tráfico urbano de carácter educativo**.

La aplicación recrea de forma simplificada el comportamiento del tráfico en una ciudad, permitiendo configurar escenarios, introducir eventos y observar estadísticas en tiempo real. El objetivo principal del proyecto es aplicar conceptos de arquitectura, gestión del estado y organización del código en un entorno Android moderno.

---

## Dónde encontrar los archivos
En la carpeta \docs se encuentra la Documentación oficial, el archivo de instalación .apk y el video demostrativo de la aplicación.

---

## Descripción general

El simulador modela elementos básicos de una ciudad como vehículos, semáforos y eventos de tráfico. Durante la ejecución, los vehículos se desplazan por un mapa en forma de cuadrícula y su comportamiento puede verse afectado por distintos eventos, como accidentes, obras, congestiones o emergencias.

El proyecto está orientado a un uso didáctico y no pretende ser una simulación realista, sino una herramienta para comprender cómo organizar y estructurar la lógica de una simulación, así como la comunicación entre las distintas capas de una aplicación Android.

---

## Arquitectura

La aplicación sigue una arquitectura **MVVM (Model–View–ViewModel)** adaptada al uso de **Jetpack Compose**, lo que permite separar claramente responsabilidades:

- **Modelo**: contiene la lógica de la simulación y la gestión de datos.
- **Vista**: se encarga de la presentación visual y de la interacción con el usuario.
- **ViewModel**: actúa como intermediario entre la vista y el modelo, gestionando el estado global de la aplicación.

Esta separación facilita la mantenibilidad del código y mejora la claridad del proyecto.

---

## Tecnologías utilizadas

- **Lenguaje**: Kotlin
- **Plataforma**: Android
- **Interfaz de usuario**: Jetpack Compose
- **Arquitectura**: MVVM
- **Sistema de construcción**: Gradle
- **Entorno de desarrollo**: Android Studio Otter 2 Feature Drop (2025.2.2)
- **Control de versiones**: Git y GitHub
- **Pruebas**: pruebas de caja blanca y caja negra con cobertura aproximada del 65 %

---

## Estructura del proyecto

El proyecto se organiza de forma clara separando cada capa de la arquitectura:

- `model/`: lógica de la simulación y persistencia de datos.
- `view/`: pantallas y componentes visuales.
- `viewmodel/`: gestión del estado y comunicación entre vista y modelo.
- `MainActivity.kt`: punto de entrada de la aplicación.

La documentación completa del proyecto se encuentra en la carpeta `docs/`.

---

## Instalación y ejecución

Existen dos formas principales de ejecutar la aplicación.

### Opción 1: Instalación mediante APK

La forma más sencilla de probar la aplicación es instalando directamente la APK.

1. Descargar el archivo APK desde la carpeta `docs/apk/`.
2. Copiar la APK al dispositivo Android o descargarla directamente desde el navegador.
3. Permitir la instalación de aplicaciones de orígenes desconocidos en los ajustes del dispositivo, si el sistema lo solicita.
4. Abrir la APK y completar la instalación.

Una vez instalada, la aplicación puede ejecutarse como cualquier otra app Android.

---

### Opción 2: Ejecutar el proyecto desde el código

También es posible ejecutar la aplicación desde el entorno de desarrollo.

1. Clonar este repositorio.
2. Abrir el proyecto con **Android Studio Otter 2 Feature Drop (2025.2.2)**.
3. Esperar a que Gradle complete la sincronización.
4. Ejecutar la aplicación en un emulador o en un dispositivo físico conectado.

---

## Uso de la aplicación

Al iniciar la aplicación se muestra directamente la pantalla de simulación.

Para una primera experiencia más fluida se recomienda:
- Centrar el mapa de tráfico arrastrándolo en pantalla.
- Acceder a la pantalla de configuración y seleccionar el escenario de **tráfico ligero**.
- Desactivar las colisiones en las opciones avanzadas.

Desde la interfaz es posible iniciar o reiniciar la simulación, añadir eventos manualmente y consultar estadísticas en tiempo real.

---

## Pruebas

El proyecto incluye pruebas de **caja blanca**, centradas en la lógica interna de la simulación, y pruebas de **caja negra**, orientadas al comportamiento observable desde la interfaz.

Debido a las limitaciones de tiempo propias de un proyecto educativo, la cobertura de pruebas no es completa, estimándose en torno a un **65 %**, lo cual se considera adecuado para el alcance del trabajo.

---

## Documentación y recursos adicionales

En la carpeta `docs/` se incluyen los siguientes recursos:

- **Memoria del proyecto** en formato PDF.
- **APK** lista para instalar.
- **Vídeo de demostración** de la aplicación.

---

## Autores

- Andrés Alejandro Pacheco Castillo
- Luis Miguel Agra Álvarez

---

## Licencia

Proyecto desarrollado con fines educativos.
