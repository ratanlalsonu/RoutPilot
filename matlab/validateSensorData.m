function [isValid, status, cleanedData] = validateSensorData(packet, thresholds, lastTimestamp)
% VALIDATESENSORDATA Validates ESP32 or Virtual Sensor telemetry packet.
% Explicitly preserves data_source ('LIVE_HARDWARE' or 'VIRTUAL_TEST').

    isValid = true;
    status = 'VALID';
    cleanedData = packet;

    requiredFields = {'deviceId', 'timestamp', 'latitude', 'longitude', ...
                      'vibration', 'tilt', 'strain', 'displacement', ...
                      'waterLevel', 'dataSource'};

    for i = 1:length(requiredFields)
        f = requiredFields{i};
        if ~isfield(packet, f) || isempty(packet.(f))
            isValid = false;
            status = ['MISSING_FIELD_' upper(f)];
            return;
        end
    end

    if ~strcmp(packet.dataSource, 'LIVE_HARDWARE') && ~strcmp(packet.dataSource, 'VIRTUAL_TEST')
        isValid = false;
        status = 'INVALID_DATA_SOURCE';
        return;
    end

    numericVals = [packet.latitude, packet.longitude, packet.vibration, ...
                   packet.tilt, packet.strain, packet.displacement, packet.waterLevel];
    if any(isnan(numericVals)) || any(isinf(numericVals))
        isValid = false;
        status = 'NAN_OR_INF_DETECTED';
        return;
    end

    if packet.latitude < -90 || packet.latitude > 90 || packet.longitude < -180 || packet.longitude > 180
        isValid = false;
        status = 'INVALID_GPS_COORDINATES';
        return;
    end

    if packet.vibration < thresholds.vibration.validMin || packet.vibration > thresholds.vibration.validMax || ...
       packet.tilt < thresholds.tilt.validMin || packet.tilt > thresholds.tilt.validMax || ...
       packet.strain < thresholds.strain.validMin || packet.strain > thresholds.strain.validMax || ...
       packet.displacement < thresholds.displacement.validMin || packet.displacement > thresholds.displacement.validMax || ...
       packet.waterLevel < thresholds.waterLevel.validMin || packet.waterLevel > thresholds.waterLevel.validMax
        isValid = false;
        status = 'OUT_OF_RANGE';
        return;
    end

    if nargin >= 3 && ~isempty(lastTimestamp)
        if packet.timestamp == lastTimestamp
            isValid = false;
            status = 'DUPLICATE_PACKET';
            return;
        end
    end
end
