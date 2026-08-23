package com.tunjid.snapshottable.compat.k250_dev_3513

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k250_dev_498.CompatContextImpl as DelegateType

// 2.5.0-dev-3513 does not affect the snapshottable compat surface.
public class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.5.0-dev-3513"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
