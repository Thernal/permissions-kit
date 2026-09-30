package io.thernal.permissionskit.detektrules

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import io.thernal.permissionskit.detektrules.collections.UnsafeCollectionIndexAccess
import io.thernal.permissionskit.detektrules.packageboundary.LayerPackageBoundary
import io.thernal.permissionskit.detektrules.packageboundary.LayerPackageRequired
import io.thernal.permissionskit.detektrules.preview.PreviewMustBePrivate
import io.thernal.permissionskit.detektrules.style.ExpressionBodyNotAllowed
import io.thernal.permissionskit.detektrules.style.MultilineConstructorRequired

class ProjectRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("project")

    override fun instance() = RuleSet(
        ruleSetId,
        listOf(
            ::PreviewMustBePrivate,
            ::UnsafeCollectionIndexAccess,
            ::LayerPackageBoundary,
            ::LayerPackageRequired,
            ::ExpressionBodyNotAllowed,
            ::MultilineConstructorRequired,
        ),
    )
}
