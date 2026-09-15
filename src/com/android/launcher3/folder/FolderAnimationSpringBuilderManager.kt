/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.folder

import android.animation.AnimatorSet
import android.view.View
import com.android.launcher3.BubbleTextView
import com.android.launcher3.Hotseat
import com.android.launcher3.Workspace
import com.android.launcher3.apppairs.AppPairIcon
import com.android.launcher3.folder.ClipRevealData.Factory.getClipRevealData
import com.android.launcher3.folder.FolderAnimationData.Factory.getAnimationData
import com.android.launcher3.folder.FolderGridOrganizer.createFolderGridOrganizer
import com.android.launcher3.folder.IconAnimationData.Factory.getIconAnimationDataList
import com.android.launcher3.graphics.ShapeDelegate

/**
 * Manages the opening and closing animations for a [Folder].
 *
 * All of the animations are done in the Folder. ie. When the user taps on the FolderIcon, we
 * immediately hide the FolderIcon and show the Folder in its place before starting the animation.
 *
 * @param folder the [Folder] to animate open or closed
 * @param isOpening whether we are opening or closing the [Folder]
 */
class FolderAnimationSpringBuilderManager(
    private val folder: Folder,
    private val shapeDelegate: ShapeDelegate,
    private val launcherDelegate: LauncherDelegate,
) : FolderAnimationCreator {
    override fun createAnimatorSet(isOpening: Boolean): AnimatorSet {
        val workspace = launcherDelegate.launcher?.workspace
        val hotseat = launcherDelegate.launcher?.hotseat
        resetLauncherScale(workspace, hotseat)
        val folderAnimData: FolderAnimationData = folder.getAnimationData(isOpening)
        val clipRevealData: ClipRevealData = folder.getClipRevealData(shapeDelegate, folderAnimData)
        val iconAnimData: List<IconAnimationData> = folder.getIconAnimationDataList(folderAnimData)
        val result =
            FolderSpringAnimatorSet.build(
                folder = folder,
                launcherDelegate = launcherDelegate,
                folderAnimData = folderAnimData,
                clipRevealData = clipRevealData,
                iconAnimData = iconAnimData,
            )
        if (folder.mInfo.containerFolder != null && workspace != null && hotseat != null) {
            // This folder is nested inside another, already-open folder, which owns the
            // dim/scale effect on the background (see FolderSpringAnimatorSet.addScrimAnimators)
            // and isn't re-animating it for this nested open/close. The reset above was still
            // needed for correct position/clip math in the calls above, which ran expecting that
            // reset (unscaled) state throughout, matching the non-nested case; now that they're
            // done, restore the already-dimmed scale immediately - no animation, and still before
            // the caller starts this animator set or anything draws a frame - rather than leaving
            // the background at full undimmed scale for the entire time this nested folder is
            // open (animating it here instead would be the redundant, visibly "twitchy"
            // transition this whole nested-scrim-skip exists to avoid).
            workspace.scaleX = FolderSpringAnimatorSet.LAUNCHER_SCALE
            workspace.scaleY = FolderSpringAnimatorSet.LAUNCHER_SCALE
            hotseat.scaleX = FolderSpringAnimatorSet.LAUNCHER_SCALE
            hotseat.scaleY = FolderSpringAnimatorSet.LAUNCHER_SCALE
        }
        return result.animatorSet
    }

    // Folders can exist outside of Launcher (Ex. Transient Taskbar)
    // So we only apply Launcher effects when we have a Launcher.
    // We need to reset these values before calculating folder positioning.
    private fun resetLauncherScale(workspace: Workspace<*>?, hotseat: Hotseat?) {
        if (hotseat == null || workspace == null) return
        // Used to match the translation of the scaling between hotseat and workspace.
        workspace.setPivotToScaleWithSelf(hotseat)
        // Since we scale down workspace/hotseat when opening folder,
        // need to have initial values to find starting folder icon location
        workspace.scaleX = 1f
        workspace.scaleY = 1f
        hotseat.scaleX = 1f
        hotseat.scaleY = 1f
    }

    companion object {
        /** Returns the list of "preview items" on {@param page}. */
        fun getPreviewIconsOnPage(folder: Folder, page: Int): List<View> {
            return createFolderGridOrganizer(folder.mActivityContext.deviceProfile)
                .setFolderInfo(folder.mInfo)
                .previewItemsForPage(page, folder.iconsInReadingOrder)
        }

        /**
         * Gets the [BubbleTextView] from an icon. In some cases the BubbleTextView is the whole
         * icon itself, while in others it is contained within the view and only serves to store the
         * title text.
         */
        fun getBubbleTextView(v: View): BubbleTextView {
            return when (v) {
                is AppPairIcon -> v.titleTextView
                is FolderIcon -> v.folderName
                else -> v as BubbleTextView
            }
        }
    }
}
