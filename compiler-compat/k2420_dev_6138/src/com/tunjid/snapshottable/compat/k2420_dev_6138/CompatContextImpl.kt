package com.tunjid.snapshottable.compat.k2420_dev_6138

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k2420_dev_3583.CompatContextImpl as DelegateType

// 2.4.20-dev-6138 again changed the IrGeneratedDeclarationsRegistrar registration shape (Metro
// re-pins createIrGeneratedDeclarationsRegistrar here). That API is not on the snapshottable
// compat surface, so this is a pure chain link for precise factory selection and a future hook.
public class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.20-dev-6138"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
