# Reglas de R8 para Prompi.
# kotlinx.serialization, Room y Navigation ya incluyen sus propias reglas;
# estas líneas refuerzan la conservación de los serializadores propios.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.prompi.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.prompi.app.**$$serializer { *; }

# Los colores de ficha y el tema se guardan por nombre (base de datos, preferencias y JSON):
# se conservan intactos para que R8 no los renombre ni los convierta en enteros.
-keep enum com.prompi.app.domain.model.** { *; }
