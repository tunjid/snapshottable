package com.tunjid.snapshottable.compat.k240

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k240_beta2.CompatContextImpl as DelegateType
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

// Compiled against kotlin-compiler:2.4.0 (released). Re-pins the FirExtensionRegistrarAdapter /
// IrGenerationExtension.registerExtension call descriptors to the stable 2.4.0 shape.
// k240_dev_2124 first pinned these against 2.4.0-dev-2124, and the intervening k240_beta1 /
// k240_beta2 links are pure delegates that still carry the dev-2124 descriptor; recompiling the
// two wrappers here locks whatever shape the stable artifact exposes so a 2.4.0 host can't hit a
// NoSuchMethodError. Metro re-pins the same pair at its own k240 module. The rest of Metro's k240
// overrides are IR-annotation / diagnostic / value-parameter APIs that snapshottable does not
// wrap, so they inherit through the chain unchanged.
public open class CompatContextImpl : CompatContext by DelegateType() {

    override fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensionCompat(
        extension: FirExtensionRegistrar,
    ) {
        FirExtensionRegistrarAdapter.registerExtension(extension)
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerIrExtensionCompat(
        extension: IrGenerationExtension,
    ) {
        IrGenerationExtension.registerExtension(extension)
    }

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.0"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
