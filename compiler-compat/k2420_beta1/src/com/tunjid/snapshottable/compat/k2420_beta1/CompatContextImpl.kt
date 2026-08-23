package com.tunjid.snapshottable.compat.k2420_beta1

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k2420_dev_6138.CompatContextImpl as DelegateType

// 2.4.20-Beta1 does not affect the snapshottable compat surface.
public class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.20-Beta1"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
