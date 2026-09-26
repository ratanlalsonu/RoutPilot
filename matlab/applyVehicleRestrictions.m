function isCompatible = applyVehicleRestrictions(edge, vehicleType)
% APPLYVEHICLERESTRICTIONS Enforces vehicle-specific width, weight, height, and bridge constraints.
    isCompatible = true;
    switch upper(vehicleType)
        case 'VAN'
            if isfield(edge, 'maxWeightTons') && edge.maxWeightTons < 3.5
                isCompatible = false;
            elseif isfield(edge, 'maxHeightMeters') && edge.maxHeightMeters < 2.8
                isCompatible = false;
            elseif isfield(edge, 'minWidthMeters') && edge.minWidthMeters < 3.0
                isCompatible = false;
            elseif isfield(edge, 'vanRestricted') && edge.vanRestricted
                isCompatible = false;
            end
        case 'BIKE'
            if isfield(edge, 'bikeAccessible') && ~edge.bikeAccessible
                isCompatible = false;
            end
        case 'CAR'
            if isfield(edge, 'carAccessible') && ~edge.carAccessible
                isCompatible = false;
            end
    end
end
