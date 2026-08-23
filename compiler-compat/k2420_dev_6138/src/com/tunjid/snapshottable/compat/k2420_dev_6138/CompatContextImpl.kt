package com.tunjid.snapshottable.compat.k2420_dev_6138

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k240.CompatContextImpl as DelegateType

// 2.4.20-dev-6138 does not affect the snapshottable compat surface.
public class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.20-dev-6138"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
