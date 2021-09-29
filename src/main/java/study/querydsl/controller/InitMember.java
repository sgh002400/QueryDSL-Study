package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local") //Test에서 profile을 test로 해뒀기 때문에 InitMember가 실행되지 않는다!
@Component
@RequiredArgsConstructor
public class InitMember { //더미 데이터 추가하는 컨트롤러

    private final InitMemberService initMemberService;

    @PostConstruct //아래에 더미 데이터를 여기서 넣지 않는 이유! -> Spring LifeCycle에 의해 여기에 넣으면 @Transactional과 @PostConstruct를 동시에 사용할 수 없다.
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }
}
