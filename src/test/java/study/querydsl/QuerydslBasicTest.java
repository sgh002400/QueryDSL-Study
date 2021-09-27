package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.QMember;
import study.querydsl.domain.QTeam;
import study.querydsl.domain.Team;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.domain.QMember.*;
import static study.querydsl.domain.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

    JPAQueryFactory queryFactory;

    /**
     * 동시성 문제는 JPAQueryFactory를
     * 생성할 때 제공하는 EntityManager(em)에 달려있다. 스프링 프레임워크는 여러 쓰레드에서 동시에 같은
     * EntityManager에 접근해도, 트랜잭션 마다 별도의 영속성 컨텍스트를 제공하기 때문에, 동시성 문제는
     * 걱정하지 않아도 된다.
     */

    @BeforeEach
    public void before() {

        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {

        //member1을 찾아라.
        String qlString = "select m from Member m " + "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        //member1 찾기.
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl2() {

        //member1 찾기.
        QMember m = new QMember("m");
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) //파라미터가 알아서 안전하게 바인딩 된다!
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl3() {
        /**
         * QMember qMember = new QMember("m"); //별칭 직접 지정
         * QMember qMember = QMember.member; //기본 인스턴스 사용
         */

        //기본 인스턴스를 static import와 함께 사용
        //member1 찾기
        Member findMember = queryFactory
                .select(member) //Qmember.member -> static import 한거
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        //select, from을 selectFrom으로 합칠 수 있다.
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        /**
         * where()에 파라미터로 검색조건을 추가하면 AND 조건이 추가됨
         * 이 경우 null 값은 무시 메서드 추출을 활용해서 동적 쿼리를 깔끔하게 만들 수 있음
         */

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() { //AND 조건을 파라미터로 처리
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetch();
        assertThat(result1.size()).isEqualTo(1);
    }

    /** 모두 가능!
     * member.username.eq("member1") // username = 'member1'
     * member.username.ne("member1") //username != 'member1'
     * member.username.eq("member1").not() // username != 'member1'
     * member.username.isNotNull() //이름이 is not null
     * member.age.in(10, 20) // age in (10,20)
     * member.age.notIn(10, 20) // age not in (10, 20)
     * member.age.between(10,30) //between 10, 30
     * member.age.goe(30) // age >= 30
     * member.age.gt(30) // age > 30
     * member.age.loe(30) // age <= 30
     * member.age.lt(30) // age < 30
     * member.username.like("member%") //like 검색
     * member.username.contains("member") // like ‘%member%’ 검색
     * member.username.startsWith("member") //like ‘member%’ 검색
     */

    @Test
    public void resultFetch() {

        /**
         * fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
         * fetchOne() : 단 건 조회
            * 결과가 없으면 : null
            * 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
         * fetchFirst() : limit(1).fetchOne()
         * fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
         * fetchCount() : count 쿼리로 변경해서 count 수 조회
         */

        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();

        //처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();
                //.limit(1).fetchOne();과 동일

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();
        //results.getLimit(), getOffset() 등이 제공됨.

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면(null이면) 마지막에 출력(nulls last) 해준다.
     */
    @Test
    public void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) //nullsFirst()도 있다.
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc()) //orderBy를 넣어야 잘 작동하는지 확인하기 쉽다.
                .offset(1) //앞에 몇 개를 스킵할지 옵션. 0부터 시작(zero index)임. -> 1이면 하나 스킵함
                .limit(2) //최대 2건 조회
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {

        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults(); //이걸 붙이면 getTotal()을 사용할 수 있으므로 몇 페이지가 만들어져야 하는지 구할 수 있다.

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

        /**
         * count 쿼리가 실행되니 성능상 주의할 것!

         * 실무에서 페이징 쿼리를 작성할 때, 데이터를 조회하는 쿼리는 여러 테이블을 조인해야 하지만,
         * count 쿼리는 조인이 필요 없는 경우도 있다. 그런데 이렇게 자동화된 count 쿼리는 원본 쿼리와 같이 모두
         * 조인을 해버리기 때문에 성능이 안나올 수 있다. count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면,
         * count 전용 쿼리를 별도로 작성해야 한다.
         */
    }

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {

        List<Tuple> result = queryFactory //QueryDsl이 제공하는 Tuple임 -> 다양한 타입이 같이 있을 때 사용용
               .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()) //이런 것들을 뽑아줌
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4); //이런식으로 결과에 접근 가능
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name) //팀의 이름으로 grouping
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10+20)/2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30+40)/2
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {

        QMember member = QMember.member;
        QTeam team = QTeam.team;

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //join(조인 대상, 별칭으로 사용할 Q타입)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회하기
     */
    @Test
    public void theta_join() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //member.team이 아닌 그냥 member임에 주목
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ON절을 활용한 조인(JPA 2.1부터 지원)
     * 1. 조인 대상 필터링
     * 2. 연관관계 없는 엔티티 외부 조인
     *
     * 예) 회원과 팀을 조인하는데, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     */

    @Test
    public void join_on_filtering() throws Exception {

        List<Tuple> result = queryFactory
                .select(member, team) //select가 여러 타입이라 Tuple을 반환 값으로 받음
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA")) //member는 다 가져오는데 team은 teamA만 가져오는거
                //만약 그냥 join을 쓴다면 .where(team.name.eq("teamA")로 대체할 수 있다. 결과 동일!
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        /** 결과
        * t=[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
         * t=[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
         * t=[Member(id=5, username=member3, age=30), null]
         * t=[Member(id=6, username=member4, age=40), null]
         */

        /** 참고
         * on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면,
         * where 절에서 필터링 하는 것과 기능이 동일하다. 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때,
         * 내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.
         */
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인 - 쎄타 조인으로 안되는거 해결!
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        /**
         * 하이버네이트 5.1부터 on 을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다.
         * 물론 내부 조인도 가능하다.
         * 주의! 문법을 잘 봐야 한다. leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.

         * 일반조인: leftJoin(member.team, team)
         * on조인: from(member).leftJoin(team).on(xxx)
         */

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple=" + tuple);
        }

        /**결과
         * t=[Member(id=3, username=member1, age=10), null]
         * t=[Member(id=4, username=member2, age=20), null]
         * t=[Member(id=5, username=member3, age=30), null]
         * t=[Member(id=6, username=member4, age=40), null] -> 일반 join시 이 네개의 결과는 나오지 않음.
         * t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
         * t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
         */
    }

    @Test
    public void fetchJoinNo() throws Exception {

        em.flush();
        em.clear(); //fetch join 테스트 할 때는 영속성 컨텍스트에 있는 애들을 안지워주면 결과를 제대로 보기 어렵기 때문에 깔끔히 비워줌.

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); //이미 로딩된 엔티티인지 아니면 초기화가 되지 않은 엔티티인지 알려주는 애임
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() throws Exception {

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq( //서브 쿼리의 조건과 동일한 결과를 지닌 member를 뽑음
                        select(memberSub.age.max()) //member의 나이가 가장 많은 사람(여기선 40)
                                .from(memberSub)))
                //.age.max()가 40이기 때문에 결과적으로
                .fetch();

        assertThat(result).extracting("username").containsExactly("member4");
    }

    /**
     * 나이가 평균 나이 이상인 회원
     * 서브쿼리 goe 사용
     */
    @Test
    public void subQueryGoe() throws Exception { //greater or equal

        QMember memberSub = new QMember("memberSub"); //서브 쿼리와 메인 쿼리의 alias가 겹치면 안되기 때문에 직접 생성함

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(select(memberSub.age.avg()).from(memberSub)))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in( //예제 자체는 의미 없고 이렇게 쓴다 정도
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * from 절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl도 지원하지 않는다.
     * 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다.
     * Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.

     * from 절의 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     */
    @Test
    public void selectSubQuery() {//예제 자체는 의미 없고 이렇게 쓴다 정도

        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username, //JPAExpressions -> Static import함
                       select(memberSub.age.avg())
                                .from(memberSub)).from(member).fetch();

        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " + tuple.get(select(memberSub.age.avg())
                    .from(memberSub)));
        }
    }


    /**
     * Case 문
     * select, 조건절(where), order by에서 사용 가능
     */
    @Test
    public void basicCase() { //단순한 조건

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() { //복잡한 조건

        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void goodCase() { //orderBy에서 Case 문 함께 사용하기 예제

        /**
         * 예를 들어서 다음과 같은 임의의 순서로 회원을 출력하고 싶다면?
         * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
         * 2. 0 ~ 20살 회원 출력
         * 3. 21 ~ 30살 회원 출력
         */

        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        //Querydsl은 자바 코드로 작성하기 때문에 rankPath 처럼 복잡한 조건을 변수로 선언해서 select 절, orderBy 절에서 함께 사용할 수 있다.

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);

            /** 결과
             * username = member4 age = 40 rank = 3
             * username = member1 age = 10 rank = 2
             * username = member2 age = 20 rank = 2
             * username = member3 age = 30 rank = 1
             */
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        // 위와 같이 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다. 상수를 더하는 것 처럼 최적화가 어려우면 SQL에 constant 값을 넘긴다.

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        
        //{username}_{age}를 만들고 싶음
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) //age와 _는 타입이 다르기 때문에 type cast 해줌.
                //문자가 아닌 다른 타입들은 stringValue()로 문자로 변환할 수 있다.
                // 이 방법은 ENUM을 처리할 때도 자주 사용한다.
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /** 프로젝션이란? - select 절에 어떤 값을 가져올지 정하는거
     * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
     */
    @Test
    public void tupleProjection() {

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }

    @Test
    public void findDtoByJPQL() {

        List<MemberDto> result = em.createQuery(
                        "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        /** 단점
         * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
         * DTO의 package이름을 다 적어줘야해서 지저분함
         * 생성자 방식만 지원함
         */
    }

    /** QueryDsl로 결과를 DTO 반환할 때 사용하는 방식
     * 프로퍼티 접근
     * 필드 직접 접근
     * 생성자 사용
     */
    @Test
    public void findDtoBySetter() {

        //1. 프로퍼티 접근 - Setter를 통해 접근

        //QueryDsl이 일단 MemberDto를 생성하고 값을 set하기 때문에 기본 생성자가 필요하다!
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() {

        //2. 필드 직접 접근

        //MemberDto의 필드에 값을 바로 넣어준다(setter 필요 없음). 마찬가지로 기본 생성자 필요함!
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByField() {

        //별칭이 다를 때 - 필드 방식은 필드명이 맞아야 들어가고 setter 방식은 프로퍼티명이 맞아야 들어간다.
        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.username.as("name"), //.as로 별칭을 만들어준다. / 이 부분도 ExpressionUtils.as(member.username, "name").as("name"),로 쓸 수 있긴 하다.
                                ExpressionUtils.as( //서브 쿼리를 사용할 때 이름이 없어서 alias를 줘야 하는 상황임.
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age")))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByConstructor() {

        //3. 생성자 사용

        //MemberDto의 생성자를 사용해서 넣어주는 방식. 생성자와 타입이 맞아야 들어간다. -> 타입을 보고 값을 넣기 때문에 이름이 달라도 상관 없다.
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void findDtoByQueryProjection() {

        //생성자 사용 방법 dto도 Q파일로 만들어서 사용하는 방법

        //DTO에 QueryDSL 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다.
        //QueryDsl에 의존성이 생기므로 라이브러리를 뺐을 때 코드가 영향을 받는다.
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age)) //타입이 다르거나 생성자와 안맞으면 컴파일 시점에 오류를 내줌 -> 실제로 생성자도 호출함
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    /**
     * 적 쿼리를 해결하는 두가지 방식
     * BooleanBuilder
     * Where 다중 파라미터 사용
     */
    @Test
    public void dynamic_BooleanBuilder() throws Exception {

        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) { //파라미터의 값이 null이냐 아니냐에 따라 쿼리가 동적으로 바뀌어야 하는 상황임

       BooleanBuilder builder = new BooleanBuilder(); //앞에 Null이 들어오지 않게 코드를 작성한다는 가정하에 초기값을 넣어둘 수도 있다.
        //BooleanBuilder(member.username.eq(usernameCond)) 이런식으로

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        //두 개의 값이 모두 있다면 and 조건으로 쿼리에 파라미터 바인딩이 되지만 만약 null 인게 있다면 쿼리에 포함이 되지 않는다.

        return queryFactory
                .selectFrom(member)
                .where(builder) //where에 builder를 넣음으로써 해결! / 이 builder 역시 and, or 조립이 가능하다
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() throws Exception {

        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond)) //만약 아래 메서드들에서 null이 반환되면 그냥 무시가 된다.
                //.where(allEq(usernameCond, ageCond) 이런식으로도 사용 가능
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) { //메서드로 만들었기 때문에 다른 쿼리에서도 재활용 할 수 있다는 장점이 있다.
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond)); //Null은 잘 처리 해줘야됨!
    }

    /** 수정, 삭제 벌크 연산 **/
    @Test
    //@Commit //트랜잭션이 반영되도록 해줌
    public void bulkUpdate() {

        //20살 이하는 비회원으로 변경하고 싶은 상황

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        /** 벌크 연산은 영속성 컨텍스트를 신경 쓰지 않고 DB에 바로 쿼리를 날린다.
         * 그렇기 때문에 영속성 컨텍스트와 DB의 상태가 달라지는 문제가 발생한다!
         * 하지만 DB에서 select 해서 데이터를 가져와도 영속성 컨텍스트가 우선권을 가지기 때문에 가져온 정보를 버리고 영속성 컨텍스트의 값을 택한다!!
         *
         * 해결 방법! -> 벌크 연산 후엔 em.flush(), em.clear()를 해준다!
         */

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAdd() { //멤버의 나이를 모두 한 살 더하는 벌크 쿼리
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete() { //18살 이상의 모든 멤버를 지우는 벌크 쿼리리
       long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }
}