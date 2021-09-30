package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.domain.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> { //스프링 데이터 JPA와 Querydsl 같이 쓰기
    //select m from Member m where m.username = ?
    List<Member> findByUsername(String username);

}
