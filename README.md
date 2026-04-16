<div align="center">

# 🏃‍♂️ WalkRush

**Tu compañero de caminadora. Rápido. Sencillo. Open Source.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.03.01-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/Licencia-MIT-green.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-En%20Desarrollo-orange)](https://github.com/jefrien/WalkRush)

</div>

---

## 📖 ¿Qué es WalkRush?

**WalkRush** es una aplicación Android pensada para quienes entrenan en **caminadora (treadmill)**. Nuestro objetivo es ofrecer una experiencia limpia, moderna y completamente gratuita para registrar, visualizar y mejorar tus sesiones de caminata y running indoor.

Ya sea que camines 20 minutos al día o hagas series de intervalos intensos, WalkRush quiere acompañarte con datos útiles, diseño atractivo y cero anuncios invasivos.

> 🌟 **WalkRush es 100 % Open Source.** Cualquiera puede usarla, estudiarla y mejorarla.

---

## ✨ Características

- 🔐 **Autenticación segura** con Supabase (email / contraseña).
- 🎨 **Interfaz moderna** construida con Jetpack Compose y Material 3.
- 📊 **Integración con Health Connect** para sincronizar pasos, distancia, calorías, ritmo cardíaco y más.
- 🧭 **Navegación fluida** entre Auth, Home, Historial, Perfil y Entrenamiento Activo.
- 🌙 **Arquitectura limpia** con inyección de dependencias (Koin) y separación por capas.
- 🔗 **Deep links** para callbacks de OAuth.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|------|------------|
| **UI** | Jetpack Compose + Material 3 |
| **Navegación** | Navigation Compose |
| **DI** | Koin |
| **Backend / Auth** | Supabase Kotlin SDK |
| **Red** | Ktor Client |
| **Salud** | Health Connect |
| **Serialización** | Kotlinx Serialization |
| **Build** | Gradle Kotlin DSL + KSP |

---

## 🚀 Instalación para desarrollo

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/jefrien/WalkRush.git
   cd WalkRush
   ```

2. **Abre el proyecto en Android Studio**
   - Recomendado: *Android Studio Ladybug o superior*.

3. **Configura tus credenciales de Supabase**
   Crea o edita el archivo `local.properties` en la raíz del proyecto:
   ```properties
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY=tu-anon-key-publica
   ```

4. **Sincroniza y ejecuta**
   - Dale a *"Sync Project with Gradle Files"* ▶️
   - Selecciona un dispositivo con **API 26+** y presiona *Run*.

---

## 🤝 ¿Cómo puedes ayudar?

WalkRush está en una etapa temprana y **toda contribución cuenta**. No necesitas ser un experto en Android para aportar:

### 💻 Código
- Implementar las pantallas faltantes (`Home`, `Historial`, `Perfil`, `Entrenamiento Activo`).
- Mejorar la experiencia de autenticación (login social, recuperar contraseña).
- Optimizar la integración con Health Connect.
- Agregar tests unitarios y de UI.
- Refactorizar y proponer mejoras arquitectónicas.

### 🎨 Diseño
- Crear mockups, iconos o ilustraciones para la app.
- Proponer paletas de colores accesibles.
- Diseñar el flujo de usuario (UX) para entrenamientos guiados.

### 📝 Documentación
- Mejorar este README.
- Escribir guías de contribución más detalladas.
- Traducir textos a otros idiomas.

### 🐛 Reportes
- Encontraste un bug? [Abre un Issue](https://github.com/jefrien/WalkRush/issues/new) describiendo el problema.
- ¿Tienes una idea? Cuéntanos en los Discussions o Issues.

### 🌟 Difusión
- Dale una estrella ⭐ al repo.
- Comparte el proyecto con amigos que usen caminadora.

---

## 🗺️ Roadmap

- [x] Estructura base del proyecto y tema visual.
- [x] Autenticación con Supabase.
- [x] Navegación principal en Compose.
- [ ] Pantalla de inicio (`Home`) con resumen de actividad.
- [ ] Pantalla de entrenamiento activo con cronómetro y métricas en vivo.
- [ ] Historial de sesiones completadas.
- [ ] Perfil de usuario y ajustes.
- [ ] Sincronización completa con Health Connect.
- [ ] Modo oscuro automático.
- [ ] Notificaciones de progreso.

---

## 📸 Capturas de pantalla

*Próximamente... ¡estamos trabajando en ello!*

Si quieres ayudar con el diseño, serás bienvenido. 🙌

---

## 📄 Licencia

WalkRush se distribuye bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

<div align="center">

**Hecho con 💙 y muchos pasos en la caminadora.**

¿Te animas a caminar con nosotros? 🚶‍♀️🚶‍♂️

</div>
