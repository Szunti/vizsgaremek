package hu.progmasters.servicebooker.service;

import hu.progmasters.servicebooker.domain.Boose;
import hu.progmasters.servicebooker.domain.SpecificPeriod;
import hu.progmasters.servicebooker.domain.WeeklyPeriod;
import hu.progmasters.servicebooker.dto.boose.BooseCreateCommand;
import hu.progmasters.servicebooker.dto.boose.BooseInfo;
import hu.progmasters.servicebooker.dto.boose.FreePeriodInfo;
import hu.progmasters.servicebooker.dto.specificperiod.SpecificPeriodCreateCommand;
import hu.progmasters.servicebooker.dto.specificperiod.SpecificPeriodInfo;
import hu.progmasters.servicebooker.dto.weeklyperiod.WeeklyPeriodCreateCommand;
import hu.progmasters.servicebooker.dto.weeklyperiod.WeeklyPeriodInfo;
import hu.progmasters.servicebooker.exceptionhandling.boose.NoSuchBooseException;
import hu.progmasters.servicebooker.exceptionhandling.specificperiod.NoSuchSpecificPeriodException;
import hu.progmasters.servicebooker.exceptionhandling.specificperiod.OverlappingSpecificPeriodException;
import hu.progmasters.servicebooker.exceptionhandling.specificperiod.SpecificPeriodNotInBooseException;
import hu.progmasters.servicebooker.exceptionhandling.weeklyperiod.NoSuchWeeklyPeriodException;
import hu.progmasters.servicebooker.exceptionhandling.weeklyperiod.OverlappingWeeklyPeriodException;
import hu.progmasters.servicebooker.exceptionhandling.weeklyperiod.WeeklyPeriodNotInBooseException;
import hu.progmasters.servicebooker.repository.BooseRepository;
import hu.progmasters.servicebooker.repository.SpecificPeriodRepository;
import hu.progmasters.servicebooker.repository.WeeklyPeriodRepository;
import hu.progmasters.servicebooker.util.DayOfWeekTime;
import hu.progmasters.servicebooker.util.interval.Interval;
import hu.progmasters.servicebooker.util.interval.IntervalSet;
import hu.progmasters.servicebooker.util.period.PeriodInterval;
import hu.progmasters.servicebooker.util.period.SimplePeriod;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static hu.progmasters.servicebooker.util.interval.Interval.interval;
import static hu.progmasters.servicebooker.util.period.PeriodInterval.periodInterval;

@Service
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class BooseService {

    private final BooseRepository booseRepository;
    private final WeeklyPeriodRepository weeklyPeriodRepository;
    private final SpecificPeriodRepository specificPeriodRepository;
    private final DateTimeBoundChecker dateTimeBoundChecker;

    private final ModelMapper modelMapper;

    public BooseService(BooseRepository booseRepository,
                        WeeklyPeriodRepository weeklyPeriodRepository,
                        SpecificPeriodRepository specificPeriodRepository, DateTimeBoundChecker dateTimeBoundChecker, ModelMapper modelMapper) {
        this.booseRepository = booseRepository;
        this.weeklyPeriodRepository = weeklyPeriodRepository;
        this.specificPeriodRepository = specificPeriodRepository;
        this.dateTimeBoundChecker = dateTimeBoundChecker;
        this.modelMapper = modelMapper;
    }

    public BooseInfo save(BooseCreateCommand command) {
        Boose toSave = modelMapper.map(command, Boose.class);
        Boose saved = booseRepository.save(toSave);
        return modelMapper.map(saved, BooseInfo.class);
    }

    public BooseInfo findById(int id) {
        Boose boose = getFromIdOrThrow(id);
        return modelMapper.map(boose, BooseInfo.class);
    }

    public List<BooseInfo> findAll() {
        return booseRepository.findAll().stream()
                .map(boose -> modelMapper.map(boose, BooseInfo.class))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WeeklyPeriodInfo addWeeklyPeriodForBoose(int booseId, WeeklyPeriodCreateCommand command) {
        Boose boose = getFromIdOrThrow(booseId);
        WeeklyPeriod toSave = modelMapper.map(command, WeeklyPeriod.class);
        toSave.setBoose(boose);

        booseRepository.lockForUpdate(boose);
        // this check needs to read already committed, that is why the isolation level is set
        if (!weeklyPeriodRepository.findOverlappingPeriods(boose, toSave).isEmpty()) {
            throw new OverlappingWeeklyPeriodException();
        }
        WeeklyPeriod saved = weeklyPeriodRepository.save(toSave);
        return modelMapper.map(saved, WeeklyPeriodInfo.class);
    }

    public List<WeeklyPeriodInfo> findAllWeeklyPeriodsForBoose(int booseId) {
        Boose boose = getFromIdOrThrow(booseId);
        return weeklyPeriodRepository.findAllOrderedFor(boose).stream()
                .map(weeklyPeriod -> modelMapper.map(weeklyPeriod, WeeklyPeriodInfo.class))
                .collect(Collectors.toList());
    }

    public WeeklyPeriodInfo findWeeklyPeriodForBooseById(int booseId, int id) {
        WeeklyPeriod weeklyPeriod = getWeeklyPeriodFromBooseAndIdOrThrow(booseId, id);
        return modelMapper.map(weeklyPeriod, WeeklyPeriodInfo.class);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SpecificPeriodInfo addSpecificPeriodForBoose(int booseId, SpecificPeriodCreateCommand command) {
        dateTimeBoundChecker.checkInBound(interval(command.getStart(), command.getEnd()));
        Boose boose = getFromIdOrThrow(booseId);
        SpecificPeriod toSave = modelMapper.map(command, SpecificPeriod.class);
        toSave.setBoose(boose);

        booseRepository.lockForUpdate(boose);
        // this check needs to read already committed that is why the isolation level is set
        if (!specificPeriodRepository.findOverlappingPeriods(boose, toSave).isEmpty()) {
            throw new OverlappingSpecificPeriodException();
        }
        Interval<LocalDateTime> periodInterval = interval(command.getStart(), command.getEnd());
        // TODO return if weekly is replaced, partially covered or neither
//        IntervalSet<LocalDateTime> expandedWeeklyPeriods = expandWeeklyPeriods(boose, periodInterval);
        SpecificPeriod saved = specificPeriodRepository.save(toSave);
        return modelMapper.map(saved, SpecificPeriodInfo.class);
    }

    public List<SpecificPeriodInfo> findAllSpecificPeriodsForBoose(int booseId, Interval<LocalDateTime> interval,
                                                                   Boolean bookable) {
        Interval<LocalDateTime> constrainedInterval = dateTimeBoundChecker.constrain(interval);
        Boose boose = getFromIdOrThrow(booseId);
        return specificPeriodRepository.findAllOrderedFor(boose, constrainedInterval, bookable).stream()
                .map(specificPeriod -> modelMapper.map(specificPeriod, SpecificPeriodInfo.class))
                .collect(Collectors.toList());
    }

    public SpecificPeriodInfo findSpecificPeriodForBooseById(int booseId, int id) {
        SpecificPeriod specificPeriod = getSpecificPeriodFromBooseAndIdOrThrow(booseId, id);
        return modelMapper.map(specificPeriod, SpecificPeriodInfo.class);
    }

    public List<FreePeriodInfo> getFreePeriodsForBoose(int booseId, Interval<LocalDateTime> interval) {
        Interval<LocalDateTime> constrainedInterval = dateTimeBoundChecker.constrain(interval);
        Boose boose = getFromIdOrThrow(booseId);
        IntervalSet<PeriodInterval, LocalDateTime> weeklyPeriods = expandWeeklyPeriods(boose, constrainedInterval);

        List<SpecificPeriod> specificPeriodList =
                specificPeriodRepository.findAllOrderedFor(boose, constrainedInterval, null);

        IntervalSet<PeriodInterval, LocalDateTime> specificPeriodsToRemove = new IntervalSet<>();
        IntervalSet<PeriodInterval, LocalDateTime> specificPeriodsToAdd = new IntervalSet<>();

        for (SpecificPeriod specificPeriod : specificPeriodList) {
            PeriodInterval periodInterval = periodInterval(specificPeriod);
            if (specificPeriod.isBookable()) {
                specificPeriodsToAdd.addWithoutChecks(periodInterval);
            }
            specificPeriodsToRemove.addWithoutChecks(periodInterval);
        }

        weeklyPeriods.subtract(specificPeriodsToRemove);
        weeklyPeriods.addAllWithoutChecks(specificPeriodsToAdd);

        return weeklyPeriods.stream()
                .map(periodInterval ->
                        modelMapper.map(periodInterval.getPeriod(), FreePeriodInfo.class))
                .collect(Collectors.toList());
    }

    private IntervalSet<PeriodInterval, LocalDateTime> expandWeeklyPeriods(Boose boose,
                                                                           Interval<LocalDateTime> queriedInterval) {
        IntervalSet<PeriodInterval, LocalDateTime> result = new IntervalSet<>();

        List<WeeklyPeriod> weeklyPeriods = weeklyPeriodRepository.findAllOrderedFor(boose);

        if (weeklyPeriods.isEmpty()) {
            return result;
        }

        LocalDateTime currentDateTime = queriedInterval.getStart();
        DayOfWeekTime currentWeekTime = DayOfWeekTime.from(currentDateTime);

        // set firstToExpand
        WeeklyPeriod lastWeeklyPeriod = weeklyPeriods.get(weeklyPeriods.size() - 1);
        boolean lastPeriodCrossesWeekBoundary = lastWeeklyPeriod.crossesWeekBoundary();

        ListIterator<WeeklyPeriod> firstToExpand = null;
        if (lastPeriodCrossesWeekBoundary && currentWeekTime.sameWeekBefore(lastWeeklyPeriod.getEnd())) {
            firstToExpand = weeklyPeriods.listIterator(weeklyPeriods.size() - 1);
        } else {
            ListIterator<WeeklyPeriod> iterator = weeklyPeriods.listIterator();
            while (iterator.hasNext()) {
                WeeklyPeriod firstToExpandCandidate = iterator.next();
                if (currentWeekTime.sameWeekBefore(firstToExpandCandidate.getEnd())) {
                    iterator.previous();
                    firstToExpand = iterator;
                    break;
                }
            }
            if (firstToExpand == null) {
                if (lastPeriodCrossesWeekBoundary) {
                    iterator.previous();
                    firstToExpand = iterator;
                } else {
                    firstToExpand = weeklyPeriods.listIterator();
                }
            }
        }

        ListIterator<WeeklyPeriod> expansionIterator = firstToExpand;
        while (true) {
            WeeklyPeriod toExpand = expansionIterator.next();
            LocalDateTime start = currentDateTime.with(DayOfWeekTime.nextOrSame(toExpand.getStart()));
            LocalDateTime end = currentDateTime.with(DayOfWeekTime.next(toExpand.getEnd()));
            if (!start.isBefore(queriedInterval.getEnd())) {
                break;
            }
            result.addWithoutChecks(periodInterval(SimplePeriod.of(start, end, toExpand.getComment())));
            currentDateTime = end;
            if (!expansionIterator.hasNext()) {
                expansionIterator = weeklyPeriods.listIterator();
            }
        }
        return result;
    }

    private Boose getFromIdOrThrow(int id) {
        return booseRepository.findById(id).orElseThrow(
                () -> new NoSuchBooseException(id)
        );
    }

    private WeeklyPeriod getWeeklyPeriodFromBooseAndIdOrThrow(int booseId, int id) {
        // TODO maybe getReference is enough
        Boose boose = getFromIdOrThrow(booseId);
        WeeklyPeriod weeklyPeriod = weeklyPeriodRepository.findById(id).orElseThrow(
                () -> new NoSuchWeeklyPeriodException(id)
        );
        if (weeklyPeriod.getBoose() != boose) {
            throw new WeeklyPeriodNotInBooseException(id, booseId);
        }
        return weeklyPeriod;
    }

    private SpecificPeriod getSpecificPeriodFromBooseAndIdOrThrow(int booseId, int id) {
        // TODO maybe getReference is enough
        Boose boose = getFromIdOrThrow(booseId);
        SpecificPeriod specificPeriod = specificPeriodRepository.findById(id).orElseThrow(
                () -> new NoSuchSpecificPeriodException(id)
        );
        if (specificPeriod.getBoose() != boose) {
            throw new SpecificPeriodNotInBooseException(id, booseId);
        }
        return specificPeriod;
    }
}
