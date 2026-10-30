$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$outputPath = Join-Path $PSScriptRoot 'diagrama-estados-alarma.png'
$bitmap = [System.Drawing.Bitmap]::new(2400, 2180)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.Clear([System.Drawing.ColorTranslator]::FromHtml('#F5F7FC'))

function Brush($color) { [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($color)) }
function Text($value, $x, $y, $w, $h, $size = 28, $color = '#20314F', $bold = $false, $align = 'Center') {
    $style = if ($bold) { [System.Drawing.FontStyle]::Bold } else { [System.Drawing.FontStyle]::Regular }
    $font = [System.Drawing.Font]::new('Segoe UI', $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::$align
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $b = Brush $color
    $g.DrawString($value, $font, $b, [System.Drawing.RectangleF]::new($x, $y, $w, $h), $format)
    $b.Dispose(); $font.Dispose(); $format.Dispose()
}
function Box($x, $y, $w, $h, $fill = '#FFFFFF', $border = '#D6DFEE', $radius = 24) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = 2 * $radius
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc(($x+$w-$d), $y, $d, $d, 270, 90)
    $path.AddArc(($x+$w-$d), ($y+$h-$d), $d, $d, 0, 90)
    $path.AddArc($x, ($y+$h-$d), $d, $d, 90, 90)
    $path.CloseFigure()
    $b = Brush $fill
    $pen = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml($border), 3)
    $g.FillPath($b, $path); $g.DrawPath($pen, $path)
    $b.Dispose(); $pen.Dispose(); $path.Dispose()
}
function Arrow($coords, $color = '#657894', $dashed = $false) {
    $pen = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml($color), 4)
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    if ($dashed) { $pen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash }
    $cap = [System.Drawing.Drawing2D.AdjustableArrowCap]::new(5, 6, $true)
    $pen.CustomEndCap = $cap
    $points = [System.Drawing.PointF[]]@()
    for ($i=0; $i -lt $coords.Count; $i+=2) { $points += [System.Drawing.PointF]::new($coords[$i], $coords[$i+1]) }
    $g.DrawLines($pen, [System.Drawing.PointF[]]$points)
    $pen.Dispose(); $cap.Dispose()
}
function State($title, $detail, $x, $y, $w, $h, $fill, $border) {
    Box $x $y $w $h $fill $border
    Text $title ($x+15) ($y+13) ($w-30) 48 32 '#20314F' $true
    Text $detail ($x+18) ($y+63) ($w-36) ($h-70) 25
}

Text 'DIAGRAMA DE ESTADOS · ALARMA DE OVEJAS' 70 38 2260 85 55 '#142541' $true
Text 'MVI en Kotlin · El ciclo de alarma y los permisos evolucionan de forma independiente' 70 120 2260 55 29 '#61718B'

Box 60 210 2280 220 '#EAF0FF' '#CAD6F6'
Text 'PREFERENCIA DE AVISOS' 85 220 420 55 26 '#3A55A0' $true
State 'DESACTIVADOS' 'notificationsEnabled = false' 540 245 500 140 '#FFFFFF' '#95A9DC'
State 'ACTIVADOS' 'notificationsEnabled = true' 1510 245 500 140 '#FFFFFF' '#95A9DC'
Arrow @(1040,280,1510,280) '#4967B4'
Text 'Activar avisos' 1060 237 430 40 24 '#3A55A0'
Arrow @(1510,355,1040,355) '#4967B4'
Text 'Desactivar avisos' 1060 359 430 38 24 '#3A55A0'
Text 'Activar comprueba el permiso; si falta, inicia su solicitud. Desactivar no cancela la alarma.' 70 388 2250 34 23 '#4B608B'

Box 60 480 1080 1150
Box 1180 480 1160 1150
Text '1 · CICLO DE LA ALARMA' 90 500 1020 60 35 '#243F6D' $true
Text 'La oveja depende de este estado, incluso sin avisos.' 90 560 1020 45 25 '#61718B'
Text '2 · PERMISO DE NOTIFICACIONES' 1210 500 1100 60 35 '#243F6D' $true
Text 'Android 13+ · Permiso real comprobado al reanudar' 1210 560 1100 45 25 '#61718B'

$b = Brush '#20314F'
$g.FillEllipse($b, 588, 613, 24, 24)
$b.Dispose()
Arrow @(600,637,600,670)
State 'SIN ALARMA' "isAlarmSet = false`nOveja en reposo" 300 670 600 150 '#F0F4FA' '#A9B9D0'
State 'ALARMA ACTIVA' "isAlarmSet = true · Oveja saltando`nCambiar hora / recalcular vencimiento" 300 980 600 170 '#E8F7F4' '#4BADA0'
State 'ALARMA CUMPLIDA' "isAlarmTriggered = true`nOveja detenida · Aviso visual" 300 1320 600 155 '#FFF5DF' '#E1B453'

Arrow @(530,820,530,980) '#279A8D'
Text 'Confirmar hora' 550 865 330 45 27 '#217B71' $true
Arrow @(900,1065,1040,1065,1040,745,900,745)
Text 'Cancelar' 920 845 185 50 25 '#526883'
Arrow @(530,1150,530,1320) '#B5831D'
Text "ahora >= vencimiento`n(tick cada segundo)" 550 1180 390 90 26 '#966D17'
Arrow @(300,1400,165,1400,165,745,300,745)
Text "Descartar`nalarma" 73 1095 180 95 24 '#526883'
Text "Al cumplir: notificar solo si los avisos están activos`ny Android permite mostrar la notificación." 125 1510 950 80 25 '#61718B'

State 'SIN EVALUAR' 'NOT_REQUESTED · Estado inicial transitorio' 1320 670 890 120 '#F0F4FA' '#A9B9D0'
State 'CONCEDIDO' "GRANTED`nAvisos disponibles*" 1260 980 440 145 '#E8F7F4' '#4BADA0'
State 'RECHAZADO' "DENIED`nPuede volver a solicitarse" 1840 980 440 145 '#FFF5DF' '#E1B453'
State 'BLOQUEADO' "PERMANENTLY_DENIED`nRequiere Ajustes" 1840 1350 440 150 '#FDEDF0' '#D88D9C'

Arrow @(1765,790,1765,845,1480,845,1480,980) '#279A8D'
Text "Comprobación:`npermiso concedido" 1250 865 430 80 25 '#217B71'
Arrow @(1765,845,2060,845,2060,980) '#B5831D'
Text "Comprobación:`nsin permiso" 1855 865 420 80 25 '#966D17'
Arrow @(1940,1125,1940,1200,1580,1200,1580,1125) '#279A8D'
Text 'Solicitar y conceder' 1490 1210 530 55 24 '#217B71'
Arrow @(2180,1125,2180,1350) '#C3697D'
Text "Reintentar y`nrechazar hasta`nquedar bloqueado" 2030 1190 290 110 23 '#A94D61'
Arrow @(2060,1500,2060,1565,1370,1565,1370,1125) '#279A8D'
Text 'Conceder desde Ajustes' 1430 1512 600 44 25 '#217B71' $true
Text '* También se comprueba el bloqueo de la app y del canal.' 1230 1590 1060 30 21 '#61718B'

Box 60 1680 2280 350 '#EBF8EF' '#7BB78D'
Text 'MANEJO DE ERRORES · CORRECCIÓN' 90 1695 2220 60 33 '#23613B' $true
Text 'Una excepción controlada no es otro estado de alarma: se conserva el estado y se muestra un mensaje.' 110 1755 2180 55 27 '#345C43'
State 'CAPTURAR' 'AlarmViewModel.safely()' 145 1835 560 120 '#FFFFFF' '#7BB78D'
State 'REPORTAR' 'CrashReporter.report(error)' 920 1835 560 120 '#FFFFFF' '#7BB78D'
State 'ERROR NO FATAL' 'Adaptador Firebase propuesto' 1695 1835 560 120 '#FFFFFF' '#7BB78D'
Arrow @(705,1895,920,1895) '#358752'
Arrow @(1480,1895,1695,1895) '#358752' $true
Text 'Actual: LogCrashReporter escribe en Logcat. Pendiente: integrar Firebase y verificar recordException() en el panel.' 100 1970 2200 40 24 '#345C43'

Text 'Al volver de Ajustes se recalcula el permiso; revocarlo puede llevar a RECHAZADO o BLOQUEADO.' 80 2050 2240 45 25 '#61718B'
Text 'Alcance actual: alarma en memoria, ligada al ViewModel; no se garantiza ejecución con el proceso cerrado.' 80 2100 2240 40 24 '#61718B'

$bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bitmap.Dispose()
Write-Output $outputPath
