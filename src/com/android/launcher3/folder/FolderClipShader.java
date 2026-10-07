/*
 * Copyright (C) 2026 Zexolver
 *
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * see the LICENSE file at the root of this repository.
 */
package com.android.launcher3.folder;

import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * Shared anti-aliased clip paint for folder reveal clipping.
 *
 * <p>Lawnchair 16-dev's folder reveal clips its content/background every frame with a bare
 * {@code canvas.clipPath(path)}, which is NOT anti-aliased - it hard-cuts at the path boundary,
 * so icons near the edge look sliced during the open/close animation and only "snap back" to full
 * when the clip is removed at the end. (Launcher 15 avoided this by revealing via
 * {@code setClipToOutline}, which the platform renders anti-aliased on the GPU.)
 *
 * <p>Drawing the clip path with this paint ({@link PorterDuff.Mode#DST_IN} + anti-alias) onto an
 * offscreen layer that already contains the drawn content keeps only the covered pixels, with a
 * soft (anti-aliased) edge - the standard technique for an anti-aliased path clip. Callers wrap
 * their content draw in {@code canvas.saveLayer(...)} / {@code canvas.drawPath(path, PAINT)} /
 * {@code restoreToCount(...)}.
 */
final class FolderClipShader {

    static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    private FolderClipShader() {}
}
