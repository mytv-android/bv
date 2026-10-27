package com.github.mytv.bv.viewmodel.pgc

import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.repositories.PgcRepository
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PgcTvViewModel(
    override val pgcRepository: PgcRepository
) : PgcViewModel(
    pgcRepository = pgcRepository,
    pgcType = PgcType.Tv
)