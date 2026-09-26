function [hazardDetected, hazardType, triggeredSensors] = detectHazard(reading, thresholds)
% DETECTHAZARD Evaluates validated sensor readings against centralized thresholds.
% Supports LIVE_HARDWARE and VIRTUAL_TEST sources without mixing.

    triggeredSensors = {};
    hazardDetected = false;
    hazardType = 'NORMAL';

    vibHigh = reading.vibration >= thresholds.vibration.warning;
    tiltHigh = abs(reading.tilt) >= thresholds.tilt.warning;
    strainHigh = reading.strain >= thresholds.strain.warning;
    dispHigh = reading.displacement >= thresholds.displacement.warning;
    waterHigh = reading.waterLevel >= thresholds.waterLevel.warning;

    if vibHigh, triggeredSensors{end+1} = 'Vibration'; end
    if tiltHigh, triggeredSensors{end+1} = 'Tilt'; end
    if strainHigh, triggeredSensors{end+1} = 'Strain'; end
    if dispHigh, triggeredSensors{end+1} = 'Displacement'; end
    if waterHigh, triggeredSensors{end+1} = 'WaterLevel'; end

    count = length(triggeredSensors);
    if count == 0
        hazardDetected = false;
        hazardType = 'NONE';
        return;
    end

    hazardDetected = true;
    if vibHigh && tiltHigh && strainHigh
        hazardType = 'BRIDGE_STRUCTURAL_WARNING';
    elseif count >= 2
        hazardType = 'COMBINED_HAZARD';
    elseif vibHigh
        hazardType = 'HIGH_VIBRATION';
    elseif tiltHigh
        hazardType = 'ABNORMAL_TILT';
    elseif strainHigh
        hazardType = 'HIGH_STRAIN';
    elseif dispHigh
        hazardType = 'HIGH_DISPLACEMENT';
    elseif waterHigh
        hazardType = 'HIGH_WATER_LEVEL';
    end
end
