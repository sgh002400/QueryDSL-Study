package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.domain.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom{ //스프링 데이터 JPA와 Querydsl 같이 쓰기
    //MemberRepositoryCustom을 상속함으로써 외부에서 메서드를 사용할 수 있게 해준다.

    //select m from Member m where m.username = ?
    List<Member> findByUsername(String username);

}
