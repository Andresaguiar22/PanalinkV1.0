# panalink 🇻🇪 — Aplicación de Mensajería Premium en Tiempo Real

**panalink** es una plataforma de mensajería instantánea de alta fidelidad diseñada para dispositivos móviles utilizando **Kotlin** y **Jetpack Compose**. Sigue las pautas de diseño modernas de **Material Design 3 (M3)**, ofreciendo una experiencia fluida, intuitiva, elegante y optimizada para el rendimiento visual y la interacción táctil.

---

## ✨ Características Principales

### 💬 Mensajería Avanzada y En Tiempo Real
* **Chats 1:1 Seguros**: Comunicación directa en tiempo real con estados de entrega de mensajes.
* **Barra de Entrada Inteligente**: Rediseño completo del campo de texto con soporte multilinea y auto-expansión dinámica.
* **Mensajes de Voz Profesionales**: Grabación de notas de voz intuitiva mediante un gesto de mantener pulsado y deslizar para cancelar (con animaciones de escala y pulsación en tiempo real).

### 🎭 Panel Integrado de Medios (Emoji, GIF y Sticker)
* **Categorización Completa de Emojis**: Explora emojis de manera rápida a través de pestañas categorizadas (Smileys, Animales, Comida, Deportes, Vehículos, Objetos, Símbolos y Banderas).
* **Buscador de GIFs con Giphy**: Integración directa con la API de Giphy para buscar y enviar GIFs animados de forma instantánea.
* **Stickers Premium**:
  * Pestañas inteligentes: Recientes, Favoritos, Tendencias y categorías temáticas (Amor, Divertido, Saludos, Fiesta).
  * Zoom/Vista Previa: Mantén presionado un sticker para abrir una ventana emergente de visualización en alta definición antes de enviarlo.
  * Sistema de favoritos: Añade o quita stickers favoritos con un simple toque en el icono de corazón.

### 📸 Estados Temporales (Stories)
* Comparte momentos cotidianos mediante fotos y videos que se eliminan automáticamente después de **24 horas**.

### 🔒 Autenticación y Seguridad
* Flujo completo de registro e inicio de sesión seguro con verificación de correo electrónico.

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** [Kotlin](https://kotlinlang.org/) (100% nativo).
* **UI:** [Jetpack Compose](https://developer.android.com/compose) con Material Design 3 para un diseño adaptativo y responsivo.
* **Carga de Imágenes y Animaciones (GIF):** [Coil](https://coil-kt.github.io/coil/) (con decodificador específico `GifDecoder` para reproducción fluida).
* **Red y APIs:** [Retrofit](https://square.github.io/retrofit/) para llamadas HTTP y consumo de APIs de medios.
* **Persistencia Local:** [Room Database](https://developer.android.com/training/data-storage/room) para el almacenamiento seguro de chats, drafts y stickers favoritos.
* **Inyección de Dependencias:** Constructor Injection simplificado para un acoplamiento limpio y testeable.

---

## 🚀 Cómo Empezar (Desarrollo y Compilación)

### Requisitos Previos
* Android Studio Jellyfish o superior.
* JDK 17 o superior.
* Dispositivo físico Android o Emulador con API 26 (Android 8.0) o superior.

### Configuración del Entorno
1. Duplica el archivo `.env.example` y nómbralo como `.env` en la raíz del proyecto.
2. Agrega tus credenciales necesarias (como la clave API de Giphy si deseas utilizar una propia):
   ```env
   GIPHY_API_KEY=tu_clave_api_aqui
   ```

### Compilación y Ejecución
Para compilar y empaquetar la aplicación en modo Debug, ejecuta el siguiente comando en la terminal:
```bash
gradle :app:assembleDebug
```

Para verificar errores de compilación y sintaxis:
```bash
gradle :app:compileDebugKotlin
```

---

## 🎨 Arquitectura del Proyecto

El proyecto sigue una arquitectura limpia basada en **MVVM (Model-View-ViewModel)**:

```text
app/src/main/java/com/example/
│
├── data/
│   ├── model/         # Modelos de datos (User, Message, Sticker, Giphy, etc.)
│   └── repository/    # Repositorios y fuentes de datos (Local y Remoto)
│
├── service/           # Clientes de API y Servicios (GiphyClient, GiphyApiService)
│
├── ui/
│   ├── screen/        # Pantallas compuestas (ChatScreen, LoginScreen, StatusScreen)
│   ├── theme/         # Colores, Tipografías y Temas Material 3
│   └── viewmodel/     # Controladores de estado de UI (ChatViewModel, AuthViewModel)
│
└── util/              # Clases de utilidades, manejadores multimedia y overlays
```

---

## 🛡️ Licencia

Este proyecto se distribuye bajo términos de desarrollo privado para demostraciones técnicas y portafolio.
