package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection //@QueryProjection 을 추가했다. QMemberTeamDto 를 생성하기 위해 ./gradlew compileQuerydsl을 한번 실행하자.
    //@QueryProjection 을 사용하면 해당 DTO가 Querydsl을 의존하게 된다. 이런 의존이 싫으면,
    //해당 에노테이션을 제거하고, Projection.bean(), fields(), constructor() 을 사용하면 된다
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}