# AGENTS.md

## Contexto general

Eres un **desarrollador Java senior** encargado de crear una **aplicación TPV de hostelería** para un **bar pequeño con barra y comedor**.

La aplicación debe ser:
- Rápida
- Intuitiva
- Robusta
- Pensada para uso intensivo diario

La aplicación será **desktop**, desarrollada en **Java 17+ usando JavaFX**, con **base de datos local** y funcionamiento **offline**.

---

## Mock y referencia visual (MUY IMPORTANTE)

Existe un **mock visual de referencia en Figma** que **DEBE usarse como guía principal** para el diseño y disposición de la interfaz de usuario.

🔗 **Mock de Figma:**
https://www.figma.com/make/oYmoFzIlMJnqatITEWx5uD/Aplicaci%C3%B3n-TPV-para-hosteler%C3%ADa

### Reglas respecto al mock

- El mock define:
  - Distribución de pantallas
  - Jerarquía visual
  - Flujo de uso
  - Tipología y tamaño de botones
- **No es necesario replicar el diseño pixel-perfect**, pero:
  - La estructura funcional debe respetarse
  - Las zonas principales deben existir
- La lógica de negocio **no depende del mock**, solo la UI

El diseño JavaFX debe **interpretar el mock**, no ignorarlo.

---

## Objetivo de la aplicación

Crear un **TPV de hostelería real**, orientado a:

- Gestión de ventas por **mesa**
- Visualización gráfica del comedor
- Venta en barra sin mesa
- Uso táctil
- Gestión sencilla de productos y usuarios
- Control de caja
- Informes de ventas

---

## Alcance de esta versión (V1)

### ✅ Incluido

- Gestión de mesas
- Diseño visual del comedor mediante cuadrícula
- Venta asociada a mesas o barra
- Añadir, editar y eliminar líneas de productos
- Edición de:
  - Cantidad
  - Importe total de la línea
- IVA configurable por producto (por defecto 10%)
- Venta con precios siempre introducidos con IVA
- Ventas en espera (mesas ocupadas)
- Cancelación de ventas
- Métodos de pago:
  - Efectivo
  - Tarjeta
  - Bizum
- Gestión de usuarios
- Grupos de productos y productos
- Orden configurable de productos dentro de su grupo
- Apertura y cierre de caja
- Informes de ventas:
  - Diario
  - Semanal
  - Mensual
  - Trimestral
- Interfaz moderna JavaFX + CSS inspirada en el mock de Figma

---

### ❌ Fuera de alcance explícito

- División de pagos
- Gestión de stock
- Facturación electrónica
- Integraciones externas
- Multi-tienda
- Backend remoto
- Control fiscal avanzado (TicketBAI / Verifactu)

---

## Gestión de mesas

### Mesas

Cada mesa debe tener:

- Nombre (ej: Mesa 1, Terraza A)
- Posición en cuadrícula (fila, columna)
- Estado:
  - Libre
  - Ocupada
- Una venta activa asociada

---

### Comedor (vista gráfica)

- El comedor se representa mediante una **cuadrícula visual**, similar al mock
- Cada celda puede:
  - Estar vacía
  - Contener una mesa
- Debe permitir:
  - Seleccionar mesas
  - Identificar mesas ocupadas visualmente
- El estado visual debe seguir la lógica mostrada en el mock de Figma

---

## Venta

### Características generales

- La venta puede estar asociada a:
  - Una mesa
  - Barra (venta rápida)
- Estados de la venta:
  - ABIERTA
  - EN_ESPERA
  - COBRADA
  - CANCELADA

---

### Líneas de producto

Cada línea de venta debe permitir:

- Editar cantidad
- Editar importe total
- Eliminar línea

✅ Esto es un requisito obligatorio y debe reflejarse en la UI según el mock.

---

## Precios e IVA (REGLA CRÍTICA)

- El **precio introducido por el usuario SIEMPRE incluye IVA**
- El usuario **NUNCA introduce el IVA**
- IVA por defecto: **10%**
- El sistema calcula:
  - Base imponible
  - Importe de IVA

❗ **La lógica del IVA NO DEBE ESTAR EN la UI**

---

## Interfaz de venta (según mock)

La pantalla de venta debe incluir claramente:

### 1️⃣ ZONA DE MESAS / BARRA
- Vista de mesas (comedor)
- Botón de venta en barra

### 2️⃣ ZONA DE GRUPOS DE PRODUCTOS
- Botones grandes
- Un botón por grupo
- Inspirado en disposición del mock

### 3️⃣ ZONA DE PRODUCTOS
- Se muestran al seleccionar un grupo
- Ordenados según configuración
- Botones grandes, táctiles

### 4️⃣ ZONA DE VENTA ACTUAL
- Lista de líneas
- Edición directa de cantidad e importe
- Totales visibles

---

## Grupos de productos

Cada grupo tiene:
- Nombre
- Orden visual
- Estado activo/inactivo

Sirven únicamente para organización visual (como en el mock).

---

## Productos

Cada producto debe tener:
- Nombre
- Grupo
- Precio por defecto (IVA incluido)
- Tipo de IVA
- Orden dentro del grupo
- Estado activo/inactivo

El orden determina **cómo se muestran en la pantalla de venta**, siguiendo el mock.

---

## Métodos de pago

- Efectivo
- Tarjeta
- Bizum

Una venta solo se considera cerrada cuando se paga.

---

## Caja

### Apertura
- Usuario
- Fecha/hora
- Importe inicial

### Cierre
- Resumen por método de pago
- Registro histórico

---

## Usuarios y roles

Roles:
- Administrador
- Empleado

Permisos estrictos según rol.

---

## Arquitectura recomendada

UI (JavaFX)
↳ Vistas inspiradas en el mock
↳ Controladores sin lógica de negocio

Aplicación / Casos de uso

Dominio
↳ Venta
↳ LineaVenta
↳ Mesa
↳ Producto
↳ GrupoProducto
↳ Usuario
↳ Caja

Infraestructura
↳ Persistencia local

---

## Objetivo final del agente

Crear un **TPV de hostelería funcional y visualmente claro**, usando el **mock de Figma como guía**, con especial énfasis en:

- Venta por mesas
- Edición flexible de líneas
- Flujo rápido
- UI táctil
- Correcto cálculo del IVA
