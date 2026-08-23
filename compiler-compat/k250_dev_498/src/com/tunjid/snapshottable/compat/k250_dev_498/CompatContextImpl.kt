package com.tunjid.snapshottable.compat.k250_dev_498

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k2420_beta2.CompatContextImpl as DelegateType

// 2.5.0-dev-498 does not affect the snapshottable compat surface.
public class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.5.0-dev-498"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
